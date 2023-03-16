package com.perikov.scratchlink.plans

import cats.*
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import com.perikov.scratchlink.utils.Logging

import scala.concurrent.duration.*
import com.perikov.scratchlink.JsonRPC

 



case class DiscoveryInfo()
case class DiscoveryFilter()

trait BLETypes:
  type PeripheralId
  type Error
  type BadPeripheral <: Error

  def PeripheralId(id: java.util.UUID): PeripheralId
  def PeripheralId(id: String): Either[BadPeripheral, PeripheralId]
  def PeripheralId(id: Int): PeripheralId

  given showPeripheralId: Show[PeripheralId]

trait BLESession[F[_]] extends BLETypes:
  def discover(filters: Seq[DiscoveryFilter]): fs2.Stream[F, Unit]
  def connect(peripheral: PeripheralId): Resource[F, BLEConnection[F]]

trait BLEConnection[F[_]]:
  def writeCharacteristic(
      service: String,
      characteristic: String,
      data: Array[Byte]
  ): F[Unit]
  def readCharacteristic(
      service: String,
      characteristic: String
  ): F[Array[Byte]]
  def notifications: fs2.Stream[F, Array[Byte]]

class Plans[F[_]: Monad: Temporal: Logging: MicrobitFactory]:
  def plan20230214(using
      microbitFactory: MicrobitFactory[F]
  ): F[Unit] =
    def cycle(microbit: MicroBit[F]) =
      microbit.showHeart.andWait(1.second)
        *> microbit.clearDisplay.andWait(1.second)

    microbitFactory.microbitConnection.use { cycle(_).foreverM }

  def apply(): F[Unit] = plan20230214

object Plans extends IOApp.Simple:
  import scribe.cats.*
  given MicrobitFactory[IO] = MicrobitFactory.dummy[IO]

  override def run: IO[Unit] = Plans[IO].apply()

