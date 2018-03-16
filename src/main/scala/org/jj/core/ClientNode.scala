package org.jj.core

import java.net.Socket

object Yo{
  def main(args: Array[String]): Unit = {
    println("Yo")
  }
}

class ClientNode(host: String, port: Int) extends SocketHelper {

  private val socket: Socket = new Socket(host, port)

  def ping(): Boolean = send("ping").contains("pong")
  def bye(): Boolean = send("bye").contains("bye")

  private def send(msg: String): Option[String] = {
    send(socket, msg)
    println("Client send: " + msg)

    val response = read(socket)
    println("Client received: " + response)

    response
  }
}