package com.perikov.scratchlink

import io.circe.{Json}
import com.perikov.jsonrpc.*
import fs2.Stream
import cats.data.EitherT
import com.perikov.jsonrpc.JsonRPC2Types.*
import io.circe.DecodingFailure


trait ScratchLink[F[_]]:
  import ScratchLink.*
  type Version      = String
  type PeripheralId = String
  type Res[A]       = EitherT[F, Error, A]
  type Unspecified
  def getVersion: Res[Version]
  def discover(filters: Filters): Res[Unit]
  def connect(deviceId: PeripheralId): Res[Unspecified]
  def write(): Res[Unspecified]
  def read(): Res[Unspecified]
  def startNotifications(): Res[Unspecified]
  def stopNotifications(): Res[Unspecified]
  def getServices(): Res[List[String]] //TODO: Seq?
  val notificationStream: Stream[F, Notification]

object ScratchLink:
  import io.circe.generic.semiauto.*
  import io.circe.{Encoder, JsonObject, Decoder}
  import io.circe.syntax.*
  case class Error(method: String, err: RPCError)
    extends RuntimeException(s"ScratchLink error: $method: $err")


  enum Notification:
    case DidDiscoverPeripheral(
        peripheralId: String,
        name: String,
        rssi: Double
    )
    case Unknown(value: JsonRPC2Types.Notification)

  import Notification.{*, given}
  given Decoder[DidDiscoverPeripheral]                                  = deriveDecoder
  val asScratchNotification: JsonRPC2Types.Notification => Notification =
    case JsonRPC2Types.Notification(
          "didDiscoverPeripheral",
          json: JsonObject
        ) =>
      val p =
        json.asJson.as[DidDiscoverPeripheral].toOption.get // TODO: Error
      DidDiscoverPeripheral(
        p.peripheralId,
        p.name,
        p.rssi
      )
    case n => Unknown(n)

  trait DeviceFilter:
    def encoder: Encoder.AsObject[this.type]
  object DeviceFilter:
    given Encoder.AsObject[DeviceFilter] = Encoder.AsObject.instance { f =>
      f.encoder.encodeObject(f)
    }
  case class NamePrefix(namePrefix: String) extends DeviceFilter:
    def encoder: Encoder.AsObject[this.type] =
      deriveEncoder[NamePrefix.this.type]

  case class Filters(filters: DeviceFilter*)
  object Filters:
    given Encoder.AsObject[Filters] = Encoder.AsObject.instance { f =>
      JsonObject("filters" -> Json.arr(f.filters.map(_.asJson)*))
    }

  case class ProtocolVersion(protocol: String) derives Decoder

  import cats.*
  import cats.implicits.*
  import io.circe.syntax.*
  import io.circe.Decoder
  def apply[F[_]: ScratchLink]: ScratchLink[F]          = summon
  def onRPC[F[_]: Functor](rpc: JsonRPC2[F]): ScratchLink[F] =
    import concurrent.duration.*
    given JsonRPC2.Timeout = 10.seconds //TODO: What to do with timeout? Res with context?
    new ScratchLink[F]:
      type Unspecified = Json
      def toRPCError(d: DecodingFailure, json: Json): RPCError =
        RPCError(
          s"Failure to parse incoming message: $d",
          ErrorCode.ParseError,
          json.some
        )

      def send[A: Decoder](method: String, params: Parameters): Res[A] =
        def toScratchLink(
            answer: Either[RPCError, Json]
        ): Either[Error, A] =
          val r = answer match
            case Left(value)  => Left(value)
            case Right(value) => value.as[A].leftMap(toRPCError(_, value))
          r.leftMap(Error(method, _))

        EitherT(
          rpc
            .sendRequest(method, params)
            .map(toScratchLink)
        )

      def stopNotifications(): Res[Json]                          = ???
      def connect(deviceId: PeripheralId): Res[Json]              =
        send("connect", Map("peripheralId" -> deviceId.asJson).asJsonObject)
      def discover(filters: Filters): Res[Unit]                   =
        send("discover", filters.asJsonObject)
      def getServices(): Res[List[String]]                                = 
        send("getServices", List())
      def getVersion: Res[Version]                                =
        send[ProtocolVersion]("getVersion", List()).map(_.protocol)
      val notificationStream: Stream[F, Notification] =
        import Notification.*
        rpc.notificationStream.map(asScratchNotification)
      def read(characteristicId: String): Res[Json]                                       = ???
      def startNotifications(): Res[Json]                         = ???
      def write(): Res[Json]                                      = ???

end ScratchLink
