package com.perikov.scratchlink


/** supports asking for values by a key (blocks until the value is available)
  */
trait RequestLock[F[_], K, V]:
  /** blocks indefinitely until the value for key is completed
    * @return
    *   [[None]] if the key is already in the map
    */
  def ask(k: K): F[Option[V]]

  /** @return
    *   [[true]] if the value was set for the key, [[false]] if the key is
    *   missing
    */
  def complete(k: K, v: V): F[Boolean]

object RequestLock:
  import cats.syntax.all.*
  import cats.data.OptionT
  import cats.effect.{Concurrent, Deferred, Ref}
  import cats.effect.syntax.monadCancel.*

  def apply[F[_]: Concurrent, K, V]: F[RequestLock[F, K, V]] =
    type D = Deferred[F, V]
    type M = Map[K, D]
    Ref.of(Map.empty[K, D]).map { mapRef =>
      new RequestLock[F, K, V]:

        override def ask(k: K): F[Option[V]] =
          def putToMapIfNeeded(d: D): F[Option[Deferred[F, V]]] =
            mapRef.modify { m =>
              if m.contains(k) then (m, none)
              else (m + (k -> d), d.some)
            }

          def waitForValue(d: D): F[V] = d.get.guarantee(mapRef.update(_ - k))

          val allocateDeferredForKey: F[Option[D]] =
            // deferred is allocated and discarded if the key is already in the map
            // hopefully this is a rare case
            Deferred[F, V].flatMap(putToMapIfNeeded)

          OptionT(allocateDeferredForKey).semiflatMap(waitForValue).value

        override def complete(k: K, v: V): F[Boolean] =

          def tryComplete(d: Option[D]): F[Boolean] =
            d.map(_.complete(v)).getOrElse(false.pure)

          mapRef
            .modify(m => (m - k, m.get(k)))
            .flatMap(tryComplete)
    }
end RequestLock


