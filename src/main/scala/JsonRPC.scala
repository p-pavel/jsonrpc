package com.perikov.scratchlink

/**
 * @see json rpc standard  https://www.jsonrpc.org/specification
  */
object JsonRPC:
  import io.circe.*
  import io.circe.syntax.*
  case class ErrorObject(code: Int, message: String, data: Option[Json] = None)

  trait JsonRPCMethod[Arg: Encoder, Res: Decoder]:
    def name: String
  abstract class JsonRPCMethodObject[Arg: Encoder, Res: Decoder] extends JsonRPCMethod[Arg, Res]:
    def name: String = toString()
