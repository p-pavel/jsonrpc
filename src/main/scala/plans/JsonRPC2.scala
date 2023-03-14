package com.perikov.scratchlink.plans

import scala.concurrent.duration.*

trait JsonRPC2[F[_]]:
  export JsonRPC2.{Request, Response, Notification}

  def defaultTimeout: FiniteDuration
  inline def sendRequest(method: String, request: Request): F[Response] =
    sendRequest(method, request, defaultTimeout)
  def sendRequest(
      method: String,
      request: Request,
      timeout: FiniteDuration
  ): F[Response]
  def sendNotification(method: String, notification: Request): F[Unit]
  def notificationStream: fs2.Stream[F, Notification]

object JsonRPC2:
  import io.circe.{Json, JsonObject}
  enum ErrorCode(val code: Int):
    case ParseError                           extends ErrorCode(-32700)
    case InvalidRequest                       extends ErrorCode(-32600)
    case MethodNotFound                       extends ErrorCode(-32601)
    case InvalidParams                        extends ErrorCode(-32602)
    case InternalError                        extends ErrorCode(-32603)
    case ServerError(override val code: Int)  extends ErrorCode(code)
    case UnkonwnError(override val code: Int) extends ErrorCode(code)

  case class Error(message: String, code: ErrorCode, data: Option[Json])

  type Request  = JsonObject | List[JsonObject]
  type Response = Either[Error, JsonObject]

  case class Notification(method: String, response: Response)
