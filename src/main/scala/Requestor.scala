package com.perikov.scratchlink

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import fs2.*
import scribe.LoggerSupport
import scribe.LogRecord
import scribe.message.LoggableMessage
import java.util.concurrent.TimeoutException
import scala.concurrent.duration.*

object Requestor:
  type WithId[A] = (Int, A)
  def apply[F[_]: Temporal, Req, Resp](
      con: WithId[Req] => F[Unit]
  ) =
    for
      mapRef     <- Ref.of(Map.empty[Int, Deferred[F, Resp]])
      counterRef <- Ref.of(0)
    yield new Requestor[F, Int, Req, Resp]:
      override def processResponse(id: Int, resp: Resp): F[Boolean] =
        def complete(d: Option[Deferred[F, Resp]]): F[Boolean] =
          d.map(
            _.complete(resp).reject { case false =>
              AssertionError("Internal error -- repeated completion")
            }
          ).getOrElse(false.pure)

        mapRef
          .getAndUpdate(_ - id)
          .map(_.get(id))
          .flatMap(complete)

      override def makeRequest(req: Req, timeout: FiniteDuration): F[Resp] =
        for
          counter  <- counterRef.getAndUpdate(_ + 1)
          deferred <- Deferred[F, Resp]
          _        <- mapRef.update(_ + (counter -> deferred))
          resp     <-
            (con(counter, req) >> deferred.get.timeoutTo(
              timeout,
              TimeoutException().raiseError
            )).guarantee(mapRef.update(_ - counter))
        yield resp

/** Abstraction to build request/response protocols. Sends the request and waits
  * for the response.
  */
trait Requestor[F[_], Id, Req, Resp]:

  /** make a request and wait for response
    *
    * @param req
    * @param timeout
    * @throws TimeoutException
    * @return
    */
  def makeRequest(req: Req, timeout: FiniteDuration = 100.milliseconds): F[Resp]

  /** process response when available
    *
    * @param id
    *   response id
    * @param resp
    *   response
    * @return
    *   true if the response was processed, false if unkonwn id
    */
  def processResponse(id: Int, resp: Resp): F[Boolean]
