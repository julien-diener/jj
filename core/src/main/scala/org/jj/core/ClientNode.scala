package org.jj.core

import java.net.Socket

class ClientNode(host: String, port: Int) extends SocketHelper {

  private val socket: Socket = new Socket(host, port)

  def ping(): Boolean = send("ping").contains("pong")
  def bye(): Boolean = send("bye").contains("bye")

  private def send(msg: String): Option[String] = {
    val cn = SocketConnection(socket)
    cn.sendStr(msg)
    println("Client send: " + msg)

    val response = cn.readString()
    println("Client received: " + response)

    response
  }

  def sendMsg1(msg: MsgType1): Option[String] = {
    val cn = SocketConnection(socket)
    cn.send((1: Byte) +: serialize(msg))
    cn.readString()
  }

  def sendMsg2(app: AppTest): Option[String] = {
    val cn = SocketConnection(socket)
    cn.send((2: Byte) +: serialize(app))
    cn.readString()
  }
}