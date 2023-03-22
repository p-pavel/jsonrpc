package com.perikov.scratchlink
import com.perikov.jsonrpc.{DatagramProto, JsonRPC2}
import com.perikov.utils.Logging
import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import io.circe.*
import io.circe.syntax.*
import fs2.*

import org.http4s.client.websocket.*
import org.http4s.implicits.*
import org.http4s.jdkhttpclient.*
import scribe.LoggerSupport
import scribe.LogRecord
import scribe.message.LoggableMessage
import java.util.concurrent.TimeoutException
import org.http4s.client.websocket.WSClientHighLevel
import scribe.filter.OrFilters

import com.perikov.utils.Logging
object ScratchLink:
  import _root_.io.circe.generic.semiauto.*
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

end ScratchLink

/** Example of connecting to the Scratch Link WebSocket server.
  *
  * @see
  *   https://github.com/LLK/scratch-link/blob/develop/Documentation/BluetoothLE.md
  * @see
  *   https://webbluetoothcg.github.io/web-bluetooth/
  */
object Run extends IOApp.Simple:
  import cats.*
  import cats.effect.*
  import cats.effect.implicits.*
  import cats.implicits.*
  import scribe.Scribe
  import scribe.cats.*
  import org.http4s.*
  import cats.effect.std.*
  import scala.concurrent.duration.*

  def client[F[_]: Async]: Resource[F, WSClientHighLevel[F]] =
    Resource.eval(JdkWSClient.simple)
  val endpoint                                               = uri"ws://localhost:20111/scratch/ble"

  override def run: IO[Unit] = client[IO]
    .flatMap { cl =>
      val req                     = WSRequest(endpoint)
      given WSClientHighLevel[IO] = cl
      onWebSocket[IO](req).flatMap(JsonRPC2.onDatagramProto)
    }
    .use(process)

  import fs2.*
  import scribe.Level.*

  given [F[_]: Scribe]: Scribe[Stream[F, _]]                                =
    record => Stream.eval(Scribe[F].log(record))
  extension [F[_]: FlatMap: Scribe, A](fa: F[A])
    def log(l: scribe.Level, f: A => String = ((t: A) => t.toString)): F[A] =
      fa.flatMap(a => Scribe[F].log(l, summon, f(a)).as(a))

  def onWebSocket[F[_]: Logging: Applicative](
      req: WSRequest
  )(using cl: WSClientHighLevel[F]): Resource[F, DatagramProto[F, String]] =
    cl.connectHighLevel(req).map { ws =>
      new DatagramProto[F, String]:
        def sendRequest(request: String): F[Unit]     =
          Logging[F].info("SendDatagram", "datagram" -> request) *> ws
            .send(WSFrame.Text(request))
        def notificationStream: fs2.Stream[F, String] =
          ws.receiveStream.collect { case WSFrame.Text(text, _) => text }
    }

  def testPacket(id: Any) =
    s"""{"jsonrpc":"2.0","id":$id,"method":"discover","params":{
      "filters": [
        { "namePrefix": "BBC"}
      ] 
    }}"""
  def process[F[_]: Scribe: Temporal](
      con: JsonRPC2[F]
  ): F[Unit] =
    val filters =
      ScratchLink.Filters(ScratchLink.NamePrefix("BBC")).asJsonObject
    (Stream
      .awakeEvery(1.second)
      .evalMap(i =>
        con
          .sendRequest("getVersion", List.empty)
          .log(Info, a => s"response: $a")
          .void
      )
      .take(300) ++
      Stream
        .eval(
          con
            .sendRequest(
              "discover",
              filters
            )
            .log(Info, a => s"response: $a")
            .void
        )
        .repeat
        .metered(1.second))
      // .concurrently(
      //   con.notificationStream.log(Info, a => s"received: $a")
      // )
      .compile
      .drain
end Run
