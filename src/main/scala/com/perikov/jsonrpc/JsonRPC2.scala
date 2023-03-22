package com.perikov.jsonrpc

import scala.concurrent.duration.*
import cats.effect.*
import cats.implicits.*
import cats.effect.implicits.*

import com.perikov.utils.MapLock
import fs2.Stream
import io.circe.*
import io.circe.syntax.*

trait JsonRPC2[F[_]]:
  import JsonRPC2Types.*
  import JsonRPC2.Timeout

  // TODO: provide typed request and response
  def sendRequest(method: String, params: Parameters)(using
      timeout: Timeout
  ): F[Either[RPCError, Json]]

  def sendNotification(method: String, params: Parameters): F[Unit]

  def notificationStream: fs2.Stream[F, Notification]

object JsonRPC2:

  opaque type Timeout = FiniteDuration
  object Timeout:
    given Conversion[FiniteDuration, Timeout] = identity
    given Conversion[Timeout, FiniteDuration] = identity
    given Timeout                             = 1.second
  end Timeout
  def onDatagramProto[F[_]: Concurrent: Temporal](
      proto: DatagramProto[F, String]
  ): Resource[F, JsonRPC2[F]] =
    import JsonRPC2Types.{*, given}

    type ReqId = Long
    val resources =
      (
        MapLock.create[ReqId, Either[RPCError, Json], F],
        Ref.of(0L),
        fs2.concurrent.Topic[F, Notification]
      )

    Resource
      .eval(resources.tupled)
      .flatMap { (mapLock, idRef, topic) =>
        def sendMessage(m: Message): F[Unit] =
          proto.sendRequest(m.asJson.noSpaces)

        def processIncomingMessage(m: Message): F[Option[Notification]] =
          m match
            case s: Success      =>
              mapLock.fulfill(s.id, Right(s.result)).as(None)
            case f: Failure      =>
              mapLock.fulfill(f.id, Left(f.error)).as(None)
            case r: Request      => none.pure // TODO: log
            case n: Notification => n.some.pure

        val processIncompingStream: Resource[F, Unit] =
          proto.notificationStream
            .map(parser.decode[Packet])
            .flatMap {
              case Right(packet) => Stream.emits(packet)
              case Left(_)       => Stream.empty // TODO: log
            }
            .evalMap(processIncomingMessage)
            .unNone
            .through(topic.publish)
            .compile
            .drain
            .background
            .void

        processIncompingStream.as(
          new JsonRPC2[F]:

            override def sendRequest(method: String, params: Parameters)(using
                timeout: Timeout
            ): F[Either[RPCError, Json]] =
              idRef
                .updateAndGet(_ + 1)
                .flatMap { id =>
                  mapLock
                    .reserve(id, sendMessage(Request(id, method, params)))
                    .flatMap {
                      case Some(result) => result.pure
                      case None         => AssertionError("Can't reserve id").raiseError
                    }
                    .timeout(timeout)
                }
            override def sendNotification(
                method: String,
                params: Parameters
            ): F[Unit] = sendMessage(Notification(method, params))

            override def notificationStream: Stream[F, Notification] =
              topic.subscribe(5)
        )

      }
