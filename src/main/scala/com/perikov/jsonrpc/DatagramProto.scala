package com.perikov.jsonrpc


trait DatagramProto[F[_], T]:

  def sendRequest(request: T): F[Unit]
  def notificationStream: fs2.Stream[F, T]

object DatagramProto:
  import cats.effect.kernel.Resource
  import org.http4s.client.websocket.{WSClientHighLevel, WSFrame, WSRequest}
  def onWebSocket[F[_]](
      req: WSRequest
  )(using cl: WSClientHighLevel[F]): Resource[F, DatagramProto[F, String]] =
    cl.connectHighLevel(req).map { ws =>
      new DatagramProto[F, String]:
        def sendRequest(request: String): F[Unit]     =
          ws.send(WSFrame.Text(request))
        def notificationStream: fs2.Stream[F, String] =
          ws.receiveStream.collect { case WSFrame.Text(text, _) => text }
    }
