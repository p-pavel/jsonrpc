package com.perikov.scratchlink
import utils.*
import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import fs2.*

import org.http4s.client.websocket.*
import org.http4s.implicits.*
import org.http4s.jdkhttpclient.*
import scribe.LoggerSupport
import scribe.LogRecord
import scribe.message.LoggableMessage
import java.util.concurrent.TimeoutException

def tst = cats.effect.IOLocal(0)




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

  def client[F[_]: Async]: F[WSClient[F]] = JdkWSClient.simple
  val endpoint                            = uri"ws://localhost:20111/scratch/ble"

  override def run: IO[Unit] = connection[IO](endpoint).use(process)


  import fs2.*
  import scribe.Level.*

  given [F[_]: Scribe]: Scribe[Stream[F, _]] =
    record => Stream.eval(Scribe[F].log(record))
  //implement mock log extension def function
  extension [F[_]:FlatMap: Scribe, A](fa: F[A])
    def log(l:scribe.Level, f: A => String = ((t: A) => t.toString)): F[A] =
      fa.flatMap(a => Scribe[F].log(l, summon, f(a)).as(a))

  def connection[F[_]: Async: Scribe](
      endpoint: Uri
  ): Resource[F, WSConnectionHighLevel[F]] =
    Resource
      .eval(client)
      .flatMap(_.connectHighLevel(WSRequest(endpoint)))
      .log(Warn, _ => s"Connected to $endpoint")

  def process[F[_]: Scribe: Concurrent](
      con: WSConnectionHighLevel[F]
  ): F[Unit] =
    con.receiveStream
      .log(Info, m => s"Received message: $m")
      .compile
      .drain
end Run

import scala.concurrent.duration.*





