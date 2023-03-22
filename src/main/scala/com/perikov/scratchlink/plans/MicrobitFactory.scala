package com.perikov.scratchlink.plans

import cats.*
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import com.perikov.utils.Logging
import scala.concurrent.duration.*

trait MicrobitFactory[F[_]]:
  def microbitConnection: Resource[F, MicroBit[F]]


object MicrobitFactory:
  transparent inline def apply[F[_]](using
      microbitFactory: MicrobitFactory[F]
  ) =
    microbitFactory

  def dummy[F[_]: Logging: Monad]: MicrobitFactory[F] =
    new MicrobitFactory[F]:
      import Logging.apply.*

      def microbitConnection: Resource[F, MicroBit[F]] =
        Resource.make(info("Opening connection").as(new MicroBit[F]:
          def showHeart: F[Unit]    = info("showHeart", "heart" -> "♥")
          def clearDisplay: F[Unit] = info("clearDisplay")
        ))(_ => info("Closing connection"))

