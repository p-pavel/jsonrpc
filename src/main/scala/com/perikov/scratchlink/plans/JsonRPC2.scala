package com.perikov.scratchlink.plans

import scala.concurrent.duration.*
import cats.effect.*
import cats.data.OptionT

trait JsonRPC2[F[_]]:
  import JsonRPC2Types.*
  import JsonRPC2.Timeout

  // TODO: provide typed request and response
  def sendRequest(method: String, request: Request)(using
      timeout: Timeout
  ): F[Response]

  def sendNotification(method: String, notification: Request): F[Unit]

  def notificationStream: fs2.Stream[F, Notification]

object JsonRPC2:
  import io.circe.*

  opaque type Timeout = FiniteDuration
  object Timeout:
    given Conversion[FiniteDuration, Timeout] = identity
    given Conversion[Timeout, FiniteDuration] = identity
    given Timeout                             = 1.second
  end Timeout



