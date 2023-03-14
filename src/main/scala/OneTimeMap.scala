package com.perikov.scratchlink


trait OneTimeMap[F[_], K, V]:
  def get(k: K): F[Option[V]]
  def put(k: K, v: V): F[Boolean]
  def has(k: K): F[Boolean]