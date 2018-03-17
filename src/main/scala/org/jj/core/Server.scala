package org.jj.core

import java.io.{BufferedInputStream, File, FileOutputStream, PrintStream}
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
  protected def read(socket: Socket): Option[String] = {
    val reader = new BufferedInputStream(socket.getInputStream)
    val inBytes = new Array[Byte](4096)
    val inSize = reader.read(inBytes)
    if(inSize<1) None
    else Some(new String(inBytes, 0, inSize))
  }

  protected def send(socket: Socket, msg: String): Unit = {
    println(s"  sending msg + '$msg'")
    val out = socket.getOutputStream
    out.write(msg.getBytes)
    out.flush()
    println(s"  msg sent")
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
    read(socket) match {
      case Some(request) =>
        // respond
        request.toLowerCase match {
          case "ping" => reply("pong")
          case "bye"  => reply("bye"); quit("on client request")
          case r      => reply(s"""unknown request "$r" """)
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
    send(socket, msg)
  } catch {
    case NonFatal(t) =>
      val stack = t.getStackTrace.mkString("\n")
      quit(s" - unexpected error: ${t.getMessage}\n" + stack)
  }

}