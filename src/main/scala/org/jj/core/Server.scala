package org.jj.core

import java.io.{BufferedInputStream, File, FileOutputStream, PrintStream}
import java.net.{ServerSocket, Socket, SocketException}


object Server {

  def main(args: Array[String]): Unit = {
    val port = args(0).toInt
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
  protected def read(socket: Socket): String = {
    val reader = new BufferedInputStream(socket.getInputStream)
    val inBytes = new Array[Byte](4096)
    val inSize = reader.read(inBytes)
    new String(inBytes, 0, inSize)
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
    // This will block until a connection comes in.
    val socket = serverSocket.accept()
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
    try{
      // read request
      val request = read(socket)

      // respond
      val ans = request.toLowerCase match {
        case "bye"  => continue = false; "bye"
        case "ping" => "pong"
        case r => s"""unknown request "$r" """
      }
      send(socket, ans)

    } catch {
      case _: SocketException if closeRequested =>
        println(s"connection with ${socket.getRemoteSocketAddress} closed on server request")
    }
  }

  def close(): Unit = {
    continue = false
    closeRequested = true
    // is processing ?
    socket.close()
  }
}