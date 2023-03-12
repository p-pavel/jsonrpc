package com.perikov.scratchlink

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import utils.*

import fs2.*
import scribe.*
import java.util.concurrent.TimeoutException
import scala.concurrent.duration.*

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
  def processResponse(id: Id, resp: Resp): F[Boolean]

object Requestor:
  import scribe.Level.*
  type Id = Long

  type WithId[A] = (Id, A)

  def apply[F[_]: Temporal: Scribe, Req, Resp](
      con: WithId[Req] => F[Unit]
  ): F[Requestor[F, Id, Req, Resp]] =
    for
      mapRef     <- Ref.of(Map.empty[Id, Deferred[F, Resp]])
      counterRef <- Ref.of(0l)
    yield new Requestor[F, Id, Req, Resp]:
      override def processResponse(id: Id, resp: Resp): F[Boolean] =
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

      //give example of using scribe with cats and MDC  
      override def makeRequest(req: Req, timeout: FiniteDuration): F[Resp] =
        for
          _ <- Scribe[F].apply("a"->"b"){
            Scribe[F].trace( "makeRequest")
          }
          counter  <- counterRef.getAndUpdate(_ + 1)
          deferred <- Deferred[F, Resp]
          _        <- mapRef.update(_ + (counter -> deferred))
          resp     <-
            (con(counter, req) >> deferred.get.timeoutTo(
              timeout,
              TimeoutException().raiseError
            )).guarantee(mapRef.update(_ - counter))
        yield resp
