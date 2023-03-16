package com.perikov.jsonrpc

import cats.Eq
import com.perikov.utils.macros.*
import io.circe.{Codec, Json, JsonObject}
import io.circe.syntax.*
import io.circe.testing.{ArbitraryInstances, CodecTests}
import munit.{Location, TestOptions}
import org.scalacheck.{Arbitrary, Gen}

import scala.quoted.*

import com.perikov.jsonrpc.JsonRPC2Types
class JsonRPCCodecsSuite extends munit.DisciplineSuite, ArbitraryInstances:
  import JsonRPC2Types.{*, given}
  override protected def scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(200)
  given Arbitrary[Parameters]                     = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[JsonObject],
      Arbitrary.arbitrary[List[Json]]
    )
  )
  given Arbitrary[Request]                        = Arbitrary(
    for
      id     <- Arbitrary.arbitrary[Long]
      method <- Arbitrary.arbitrary[String]
      params <- Arbitrary.arbitrary[Parameters]
    yield Request(id, method, params)
  )
  given Arbitrary[Notification]                   = Arbitrary(
    for
      method <- Arbitrary.arbitrary[String]
      params <- Arbitrary.arbitrary[Parameters]
    yield Notification(method, params)
  )

  given Arbitrary[Success] = Arbitrary(
    for
      id     <- Arbitrary.arbitrary[Long]
      result <- Arbitrary.arbitrary[Json]
    yield Success(id, result)
  )

  given Arbitrary[ErrorCode] = Arbitrary(
    Arbitrary.arbitrary[Int].map(RPCError.decode)
  )

  given Arbitrary[RPCError] = Arbitrary(
    for
      message <- Arbitrary.arbitrary[String]
      code    <- Arbitrary.arbitrary[ErrorCode]
      js      <- Arbitrary.arbitrary[Json].filterNot(_.isNull)
      data    <- Gen.option(js)
    yield RPCError(message, code, data)
  )
  given Arbitrary[Failure]  = Arbitrary(
    for
      id    <- Arbitrary.arbitrary[Long]
      error <- Arbitrary.arbitrary[RPCError]
    yield Failure(id, error)
  )

  given Arbitrary[Message]                                            = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[Request],
      Arbitrary.arbitrary[Notification],
      Arbitrary.arbitrary[Success],
      Arbitrary.arbitrary[Failure]
    )
  )
  inline def checkCodec[A: Codec.AsObject: TypeName: Arbitrary: cats.Eq]: Unit =
    checkAll(s"Codec[${typeName[A]}]", CodecTests[A].codec)


  checkCodec[Message]