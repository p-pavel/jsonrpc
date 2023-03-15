package com.perikov.scratchlink.plans

trait MapLock[F[_], K, V]:
  /** @param action
    *   action to be performed if the key is reserved successfully. for example,
    *   sending a request
    * @return
    *   [[None]] if the key cannot be reserved, e.g. it is already reserved
    *   [[Some(v)]] the value after [[fulfill]] is called
    */
  def reserve(action: F[Unit])(key: K): F[Option[V]]

  /** the key is removed if it is present
    * 
   * @return
    *   [[true]] if the key was present and the value was fulfilled
    * 
    */
  def fulfill(key: K, value: V): F[Boolean]

object MapLock: 
  import cats.*
  import cats.data.OptionT
  import cats.implicits.*
  import cats.effect.*
  import cats.effect.implicits.*

  import cats.effect.std.MapRef

  def apply[F[_], K, V](using F: MapLock[F, K, V]): MapLock[F, K, V] = F

  def create[K, V, F[_]: Concurrent]: F[MapLock[F, K, V]] =
    type M = Map[K, V]
    type D = Deferred[F, V]
    Ref
      .of(Map.empty[K, D])
      .map(refMap =>
        
        new MapLock[F, K, V]:

          override def reserve(action: F[Unit])(key: K): F[Option[V]] =
            def processDeferred(d: D) = d.get.guarantee(refMap.update(_ - key))
            val allocDeferred: F[Option[Deferred[F, V]]]         =
              Deferred[F, V].flatMap(d =>
                refMap.modify ( m =>
                  if m.contains(key) then (m, none)
                  else (m + (key -> d), d.some)
                )
              )
            OptionT(allocDeferred).semiflatMap(processDeferred).value
          override def fulfill(key: K, value: V): F[Boolean]          =
            OptionT(refMap.getAndUpdate(_ - key).map(_.get(key)))
              .semiflatMap(_.complete(value))
              .getOrElse(false)
      )
end MapLock
