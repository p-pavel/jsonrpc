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
    given Timeout                             = 1000.second
  end Timeout
  def onDatagramProto[F[_]: Concurrent: Temporal](
      proto: DatagramProto[F, String]
  ): Resource[F, JsonRPC2[F]] =
    import JsonRPC2Types.{*, given}
    type ReqId = Long
    val resources =
      (MapLock.create[ReqId, Either[RPCError, Json], F], Ref.of(0L))

    Resource.eval(resources.tupled).map { (mapLock, idRef) =>
      def sendMessage(m: Message): F[Unit] = proto.sendRequest(m.asJson.noSpaces)

      new JsonRPC2[F]:

        override def sendRequest(method: String, params: Parameters)(using
            timeout: Timeout
        ): F[Either[RPCError, Json]] =
          idRef
            .updateAndGet(_ + 1)
            .flatMap { id =>
              mapLock
                .reserve(id, sendMessage(Request(id, method, params)))
                .timeout(timeout)
                .flatMap {
                  case Some(result) => result.pure
                  case None         => AssertionError("Can't reserve id").raiseError
                }
            }
        override def sendNotification(
            method: String,
            params: Parameters
        ): F[Unit] = sendMessage(Notification(method, params))

        override def notificationStream: Stream[F, Notification] =
          proto.notificationStream
            .map {
              parser.decode[Packet]
            }
            .flatMap {
              case Right(packet) => Stream.emits(packet)
              case Left(_)       => Stream.empty // TODO: log
            }
            .flatMap {
              case n: Notification => Stream.emit(n)
              case s: Success      =>
                Stream.exec(
                  mapLock.fulfill(s.id, Right(s.result)).void
                ) // TODO: log orphane responses
              case f: Failure      =>
                Stream
                  .exec(mapLock.fulfill(f.id, Left(f.error)).void) // TODO: log
              case r: Request      => Stream.empty // TODO: log
            }

    }
