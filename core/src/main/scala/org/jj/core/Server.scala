package org.jj.core

import java.net.{ServerSocket, Socket, SocketException}

import scala.util.control.NonFatal




/** connect to a client, answer message until connection is closed
  * then, connect to next client
  */
trait Server extends SocketHelper with Runnable {

  def port: Int
  def poolSize: Int

  // answer a connection and return None or Some(quit message) to stop connection
  def answer(connection: SocketConnection): Option[String]

  private val serverSocket = new ServerSocket(port)
  private var socket: Socket = _
  private var continue = true

  def start(): Unit = while (continue) try {
    if(socket == null)
      socket = serverSocket.accept() // This will block until a connection comes in

    answer(SocketConnection(socket)).map(quit)

  } catch {
    case _: SocketException if !continue => quit("on server request")
    case NonFatal(t) => quit(s" - unexpected error: ${t.getMessage}\n" + t.getStackTrace.mkString("\n"))
  }

  private def quit(msg: String): Unit = {
    println(s"connection with ${socket.getRemoteSocketAddress} closed $msg")
    socket = null
  }

  def stop(): Unit = {
    print(s"closing service on port=$port...  ")
    continue = false
    if(socket != null) socket.close()
    serverSocket.close()
    println(s" > closed port=$port ${serverSocket.isClosed}")
  }

  override def run(): Unit = start()
}