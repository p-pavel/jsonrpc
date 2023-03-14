package com.perikov.scratchlink

import scala.util.CommandLineParser.FromString.given_FromString_Int

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

    inline def trace(inline msg: String, inline data: (String, Any)*): F[Unit] =
      log(Trace, msg, data*)
    inline def debug(inline msg: String, inline data: (String, Any)*): F[Unit] =
      log(Debug, msg, data*)
    inline def info(inline msg: String, inline data: (String, Any)*): F[Unit]  =
      log(Info, msg, data*)
    inline def warn(inline msg: String, inline data: (String, Any)*): F[Unit]  =
      log(Warn, msg, data*)
    inline def error(inline msg: String, inline data: (String, Any)*): F[Unit] =
      log(Error, msg, data*)

  object Logging:
    given fromScribe[F[_]](using sc: Scribe[F]): Logging[F] with
      import scribe.{Level as ScribeLevel}
      type Level = ScribeLevel
      val Trace = ScribeLevel.Trace
      val Debug = ScribeLevel.Debug
      val Info  = ScribeLevel.Info
      val Warn  = ScribeLevel.Warn
      val Error = ScribeLevel.Error
      val Fatal = ScribeLevel.Fatal

      def log(level: ScribeLevel, msg: String, data: (String, Any)*)(using
          Pkg,
          FileName,
          Line
      ): F[Unit] =
        val mdc = MDCMap(None)
        data foreach { case (k, v) => mdc.update(k, v.toString) }
        sc.log(level, mdc, msg)
