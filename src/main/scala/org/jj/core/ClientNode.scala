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
    sendStr(socket, msg)
    println("Client send: " + msg)

    val response = receivedStr(socket)
    println("Client received: " + response)

    response
  }

  def sendMsg1(msg: MsgType1): Option[String] = {
    send(socket, (1: Byte) +: serialize(msg))
    receivedStr(socket)
  }
  def sendMsg2(app: AppTest): Option[String] = {
    send(socket, (2: Byte) +: serialize(app))
    receivedStr(socket)
  }
}