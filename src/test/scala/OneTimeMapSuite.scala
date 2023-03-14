package com.perikov.scratchlink


import cats.*
import cats.implicits.*
import cats.laws.{IsEq, IsEqArrow}
import cats.laws.discipline.*
import org.scalacheck.Arbitrary

trait OneTimeMapLaws[F[_]: Applicative, K, V](using F: OneTimeMap[F, K, V]):
  def hasAfterPut(k: K, v: V) =
    (F.put(k, v) *> F.has(k)) <-> F.put(k, v).as(true)
  def getAfterPut(k: K, v: V) =
    (F.put(k, v) *> F.get(k)) <-> F.put(k, v).as(v.some)

  def getAfterGet(k: K, v: V) =
    (F.put(k, v) *> F.get(k) *> F.get(k)) <-> none.pure

trait OneTimeMapTests[F[_], K: Arbitrary, V: Arbitrary](
    laws: OneTimeMapLaws[F, K, V]
)(using
    F: OneTimeMap[F, K, V]
)(using Eq[F[Boolean]], Eq[F[Option[V]]])
    extends org.typelevel.discipline.Laws:
  import org.scalacheck.Prop.forAll

  def all = SimpleRuleSet(
    "OneTimeMap",
    "hasAfterPut" -> forAll(laws.hasAfterPut),
    "getAfterPut" -> forAll(laws.getAfterPut),
    "getAfterGet" -> forAll(laws.getAfterGet)
  )

object OneTimeMapTests:
  def apply[F[_]: Applicative, K: Arbitrary, V: Arbitrary](using
      F: OneTimeMap[F, K, V]
  )(using
      Eq[F[Boolean]],
      Eq[F[Option[V]]]
  ) =
    new OneTimeMapTests[F, K, V](new OneTimeMapLaws[F, K, V] {}) {}


// TODO: implement this
// class OneTimeMapSuite extends munit.DisciplineSuite:
//   checkAll("asdf", OneTimeMapTests[Option, Int, Int].all)
