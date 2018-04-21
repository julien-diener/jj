package org.jj.core

import java.io._
import java.net.{ServerSocket, Socket, SocketException}

import scala.util.control.NonFatal


object Server {

  def main(args: Array[String]): Unit = {
    val port = if(args.nonEmpty) args(0).toInt else 13013
    val filename = s"out-$port.txt"
    redirectOutput(filename)
    println(s"App starting with port $port (out = $filename)")

    val server = new Server(port, 1)

    Runtime.getRuntime.addShutdownHook(new Thread{
      override def run(): Unit = {
        println("closing on shutdown signal")
        server.close()
      }
    })

    server.run()
  }

  def redirectOutput(filename: String): Unit = {
    val file = new File(filename)
    val fos = new FileOutputStream(file, true)
    val ps = new PrintStream(fos)
    System.setOut(ps)
  }

}

trait SocketHelper{

  type MsgType1 = () => String

  protected def receivedStr(socket: Socket): Option[String] = {
    received(socket).map(b => new String(b, 0, b.length))
  }

  protected def received(socket: Socket): Option[Array[Byte]] = {
    val reader = new BufferedInputStream(socket.getInputStream)
    val inBytes = new Array[Byte](4096)
    val inSize = reader.read(inBytes)
    if(inSize<1) None
    else Some(inBytes.take(inSize))
  }

  protected def sendStr(socket: Socket, msg: String): Unit = {
    println(s"  sending msg + '$msg'")
    send(socket, msg.getBytes)
    println(s"  msg sent")
  }

  protected def send(socket: Socket, bytes: Array[Byte]): Unit = {
    val out = socket.getOutputStream
    out.write(bytes)
    out.flush()
  }

  protected def serialize[A](obj: A): Array[Byte] = {
    val bo = new ByteArrayOutputStream()
    new ObjectOutputStream(bo).writeObject(obj)
    bo.toByteArray
  }

  protected def deserialize[A](bytes:Array[Byte]): A = {
    new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject().asInstanceOf[A]
  }
}

class Server(port: Int, poolSize: Int) extends Runnable with SocketHelper {
  private val serverSocket = new ServerSocket(port)
  private var connection: ServerConnection = _


  def run() {
    connection = new ServerConnection(serverSocket)
    connection.start()
    println(s"service on port=$port stopped")
  }

  def close(): Unit = {
    if(connection != null) connection.stop()
    connection = null
    serverSocket.close()
    println(s"close service on port=$port ${serverSocket.isClosed}")
  }
}

// connect to a client and repeat until connection is closed
// then, connect to next client
class ServerConnection(serverSocket: ServerSocket) extends SocketHelper {

  private var socket: Socket = _
  private var continue = true

  def start(): Unit = while (continue) try {
    if(socket == null)
      socket = serverSocket.accept() // This will block until a connection comes in

    // read request
    received(socket) match {
      case Some(requestBytes) =>
        requestBytes(0) match {
          case 1 =>
            val f = deserialize[MsgType1](requestBytes.tail)
            reply("received ()=> " + f())

          case 2 =>
            val a = deserialize[AppTest](requestBytes.tail)
            reply("received " + a.message())

          case b =>
            val headStr = b.toString
            val request = new String(requestBytes, 0, requestBytes.length)
            request.trim.toLowerCase match {
              case "ping" => reply("pong "+headStr)
              case "bye"  => reply("bye"+headStr); quit("on client request")
              case r      => reply(s"""unknown request "$r" """+headStr)
            }
        }

      case None =>
        quit("- client has stop connection abruptly")
    }

  } catch {
    case _: SocketException if !continue => quit("on server request")
  }

  private def quit(msg: String): Unit = {
    println(s"connection with ${socket.getRemoteSocketAddress} closed $msg")
    socket = null
  }

  def stop(): Unit = {
    continue = false
    if(socket != null) socket.close()
  }

  private def reply(msg: String): Unit = try {
    sendStr(socket, msg)
  } catch {
    case NonFatal(t) =>
      val stack = t.getStackTrace.mkString("\n")
      quit(s" - unexpected error: ${t.getMessage}\n" + stack)
  }

}