package com.perikov.scratchlink

import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*

import java.util.concurrent.TimeoutException
import scribe.data.MDCMap

class RequestorTests extends munit.CatsEffectSuite:
  import scala.concurrent.duration.*
  import scribe.*
  import scribe.cats.*
  type Req  = String
  type Resp = String

  // given logger: Scribe[IO] = io.logger.withMinimumLevel(Level.Trace).f

  def requestor(
      process: Requestor.WithId[Req] => IO[Unit] = _ => IO.unit
  ): IO[Requestor[IO, Long, Req, Resp]] = Requestor[IO, Req, Resp](process)

  test("no reply") {
    requestor()
      .flatMap(r => r.makeRequest("hello", 10.milliseconds))
      .intercept[TimeoutException]
  }

  test("single reply") {
    for
      resp <- Deferred[IO, Requestor.WithId[Resp]]
      r    <- requestor((id, req) => resp.complete((id, req.reverse)).assert)
      res  <-
        (r.makeRequest("hello"), resp.get.flatMap(r.processResponse)).parTupled
      _    <- assertIO(IO(res), ("olleh", true), "Bad response")
      _    <- assertIO(
                r.processResponse(0, "hello"),
                false,
                "Should detect repeated response"
              )
    yield ()

  }

  test("unknown response") {
    requestor().flatMap(_.processResponse(1, "hello")).assertEquals(false)
  }

  test("logging") {}

  test("out of order responses") {
    // TODO: implement
  }

// object Tst extends IOApp.Simple:
//   import scribe.*
//   import scribe.cats.*
//   import scribe.data.MDC

//   override def run: IO[Unit] =
//     mdc("a" -> 1, "b" -> 2) {
//       mdc("a" -> 3, "c" -> 4) {
//         Scribe[IO].info("Logging").parReplicateA(20).void
//       }
//     }
