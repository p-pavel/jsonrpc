package com.perikov.jsonrpc


trait DatagramProto[F[_], T]:

  def sendRequest(request: T): F[Unit]
  def notificationStream: fs2.Stream[F, T]

