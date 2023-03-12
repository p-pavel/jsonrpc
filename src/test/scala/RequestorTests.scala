package com.perikov.scratchlink

import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*

import java.util.concurrent.TimeoutException

class RequestorTests extends munit.CatsEffectSuite:
  import scala.concurrent.duration.*
  type Req  = String
  type Resp = String

  def requestor(
      process: Requestor.WithId[Req] => IO[Unit] = _ => IO.unit
  ): IO[Requestor[IO, Int, Req, Resp]] = Requestor[IO, Req, Resp](process)

  test("no reply") {
    requestor()
      .flatMap(r => r.makeRequest("hello", 10.milliseconds))
      .intercept[TimeoutException]
  }

  test("single reply") {
    (for
      resp <- Deferred[IO, Requestor.WithId[Resp]]
      r    <- requestor((id, req) => resp.complete((id, req.reverse)).assert)
      res  <-
        (r.makeRequest("hello"), resp.get.flatMap(r.processResponse)).parTupled
    yield res == ("olleh", true)).assert
  }

  test("out of order responses") {
    //TODO: implement
  }
