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

    new Server(port, 1).run()
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
    val socket = serverSocket.accept() // This will block until a connection comes in
    connection = new ServerConnection(socket)
    connection.listen()
    println(s"service on port=$port stopped")
  }

  def close(): Unit = {
    if(connection != null) connection.close()
    connection = null
    serverSocket.close()
    println(s"close service on port=$port ${serverSocket.isClosed}")
  }
}

// connection to one client
class ServerConnection(socket: Socket) extends SocketHelper {
  private var continue = true
  private var closeRequested: Boolean = false

  def listen(): Unit = while (continue) {
    def quit(msg: String): Unit = {
      println(s"connection with ${socket.getRemoteSocketAddress} closed $msg")
      continue = false
    }
    try{
        // read request
        read(socket) match {
          case Some(request) =>
            // respond
            val ans = request.toLowerCase match {
              case "bye" => quit("on client request"); "bye"
              case "ping" => "pong"
              case r => s"""unknown request "$r" """
            }

            try {
              send(socket, ans)
            } catch {
              case NonFatal(t) =>
                val stack = t.getStackTrace.mkString("\n")
                quit(s" - unexpected error: ${t.getMessage}\n" + stack)
            }

          case None =>
            quit("- client has stop connection abruptly")
        }

    } catch {
      case _: SocketException if closeRequested => quit("on server request")
    }
  }

  def close(): Unit = {
    continue = false
    closeRequested = true
    // is processing ?
    socket.close()
  }
}