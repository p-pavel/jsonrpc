package com.perikov.scratchlink


object utils:
  import sourcecode.*
  import cats.*
  import cats.effect.*
  import cats.syntax.all.*
  import scribe.*
  import scribe.data.*
  import scribe.message.LoggableMessage
  trait Logging[F[_]]:
    type Level
    val Trace, Debug, Info, Warn, Error, Fatal: Level
    def log(
        level: Level,
        msg: String,
        data: (String, Any)*
    )(using Pkg, FileName, Line): F[Unit]

    inline def trace(inline msg: String, inline data: (String, Any)*): F[Unit] = log(Trace, msg, data*)
    inline def debug(inline msg: String, inline data: (String, Any)*): F[Unit] = log(Debug, msg, data*)
    inline def info(inline msg: String, inline data: (String, Any)*): F[Unit] = log(Info, msg, data*)
    inline def warn(inline msg: String, inline data: (String, Any)*): F[Unit] = log(Warn, msg, data*)
    inline def error(inline msg: String, inline data: (String, Any)*): F[Unit] = log(Error, msg, data*)



    //     MDC
    // ): F[A] =
    //   logContext(data*)(
    //   fa.flatMap(a => Scribe[F].log(level, summon, msg(a)).as(a)))

    // def logContext[F[_], A](
    //     args: =>(String, Any)*
    // )(f: Ctx ?=> F[A])(using parent: Ctx): F[A] 

    // F.delay {
    //   val mdc = MDCMap(parent.some)
    //   args.foreach((k, v) => mdc.update(k, v))
    //   mdc
    // }.flatMap { m =>
    //   f(using m)
    // }
