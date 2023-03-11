import cats.effect.IOApp
import org.http4s.client.websocket.*
import org.http4s.implicits.*
import org.http4s.jdkhttpclient.*

/** @see
  *   https://github.com/LLK/scratch-link/blob/develop/Documentation/BluetoothLE.md
  * @see
  *   https://webbluetoothcg.github.io/web-bluetooth/
  */
object Run extends IOApp.Simple:
  import cats.*
  import cats.effect.*
  import cats.effect.implicits.*
  import cats.implicits.*
  import scribe.*
  import scribe.cats.*
  import org.http4s.*
  transparent inline def log[F[_]](using ev: Scribe[F]): Scribe[F] = ev
  val client                                                       = JdkWSClient.simple[IO]
  val endpoint                                                     = uri"ws://localhost:20111/scratch/ble"

  override def run: IO[Unit] =
    for
      c <- client
      _ <- c.connect(WSRequest(endpoint)).use { ws =>
             for
               _ <- log[IO].info("Connected!")
               _ <- ws.receiveStream.compile.drain
             yield ()
           }
    yield ()
