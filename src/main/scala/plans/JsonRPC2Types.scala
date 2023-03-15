package plans

object JsonRPC2Types:
  // TODO: sending batch requests
  // TODO: id polymorphism
  import cats.*
  import cats.implicits.*
  import io.circe.{Error as CirceError, *}
  import io.circe.syntax.*
  import DecodingFailure.Reason

  private val jsonrpcVersion = "2.0"

  type Message    = Request | Notification | Response
  type Packet     = Seq[Message] // TODO: batch rules not enforced
  type Response   = Success | Failure
  case class Success(id: Long, result: Json)
  case class Failure(id: Long, error: RPCError)
  case class Request(id: Long, method: String, params: Parameters)
  case class Notification(method: String, params: Parameters)
  type Parameters = JsonObject | List[Json]

  case class RPCError(message: String, code: ErrorCode, data: Option[Json])
  enum ErrorCode(val code: Int): // TODO: use literal types

    case ParseError                           extends ErrorCode(-32700)
    case InvalidRequest                       extends ErrorCode(-32600)
    case MethodNotFound                       extends ErrorCode(-32601)
    case InvalidParams                        extends ErrorCode(-32602)
    case InternalError                        extends ErrorCode(-32603)
    // TODO: specify range for server errors (<= -32000 && >= -32099)
    case ServerError(override val code: Int)  extends ErrorCode(code)
    case UnkonwnError(override val code: Int) extends ErrorCode(code)


  given Encoder[Message] = Encoder.instance { m =>
    val map: Map[String, Json] = m match
      case Request(id, method, params)  =>
        Map(
          "id"     -> id.asJson,
          "method" -> method.asJson,
          "params" -> params.asJson
        )
      case Notification(method, params) =>
        Map("method" -> method.asJson, "params" -> params.asJson)
      case Success(id, result)          => Map("id" -> id.asJson, "result" -> result)
      case Failure(id, error)           => Map("id" -> id.asJson, "error" -> error.asJson)

    (map + ("jsonrpc" -> jsonrpcVersion.asJson)).asJson
  }

  given Codec[Parameters] = Codec.from(
    Decoder.instance { c =>
      c.as[JsonObject] orElse
        c.as[List[Json]]
    },
    Encoder.instance {
      case o: JsonObject => o.asJson
      case l: List[Json] => l.asJson
    }
  )
  object RPCError:
    def decode(c: Int): ErrorCode =
      import ErrorCode.*
      c match
        case -32700                          => ParseError
        case -32600                          => InvalidRequest
        case -32601                          => MethodNotFound
        case -32602                          => InvalidParams
        case -32603                          => InternalError
        case c if c >= -32099 && c <= -32000 =>
          ServerError(c) // TODO: use refined
        case c                               => UnkonwnError(c)
    
    given Encoder[RPCError] = Encoder.instance { e =>
      Map(
        "message" -> e.message.asJson,
        "code"    -> e.code.code.asJson,
        "data"    -> e.data.asJson
      ).asJson
    }
    given Decoder[RPCError] = Decoder.instance { c =>
      (
        c.downField("message").as[String],
        c.downField("code").as[Int].map(RPCError.decode),
        c.downField("data").as[Option[Json]]
      ).mapN(RPCError.apply)
    }
    

  given Decoder[Message] = Decoder.instance { c =>
    val versionCursor: ACursor    = c.downField("jsonrpc")
    def wrongVersion(ver: String) = DecodingFailure(
      Reason.CustomReason(s"Invalid jsonrpc version: '$ver'"),
      versionCursor
    )
    versionCursor
      .as[String]
      .ensureOr(wrongVersion)(_ == jsonrpcVersion) *>
      decodePayload(c)

  }
  given Decoder[Packet]  = Decoder.instance { c =>
    c.downArray.as[Seq[Message]].orElse(c.as[Message].map(Seq(_)))
  }

  private def decodePayload(c: ACursor): Either[DecodingFailure, Message] =
    (
      c.downField("id").as[Option[Long]],
      c.downField("method").as[Option[String]]
    ).tupled.flatMap {
      case (idOpt, Some(method)) =>
        c.downField("params")
          .as[Parameters]
          .map(params =>
            idOpt
              .map(Request(_, method, params))
              .getOrElse(Notification(method, params))
          )
      case (Some(id), _)         =>
        c.downField("error").as[RPCError].map(Failure(id, _)) orElse
          c.downField("result").as[Json].map(Success(id, _))
    }

end JsonRPC2Types