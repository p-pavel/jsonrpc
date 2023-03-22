package com.perikov.scratchlink.plans

import cats.*
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import com.perikov.utils.Logging
import scala.concurrent.duration.*

trait MicroBit[F[_]]:
  def showHeart: F[Unit]
  def clearDisplay: F[Unit]