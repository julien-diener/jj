package org.jj.core

import java.io.{BufferedInputStream, File, FileOutputStream, PrintStream}
import java.net.{ServerSocket, SocketException}
import java.util.concurrent.Executors


object App {

  def main(args: Array[String]): Unit = {
    val port = args(0).toInt
    val filename = s"out-$port.txt"
    redirectOutput(filename)
    println(s"App starting with port $port (out = $filename)")

    new NetworkService(port, 1).run()
  }

  def redirectOutput(filename: String): Unit = {
    val file = new File(filename)
    val fos = new FileOutputStream(file, true)
    val ps = new PrintStream(fos)
    System.setOut(ps)
  }

}

class NetworkService(port: Int, poolSize: Int) extends Runnable {
  private val serverSocket = new ServerSocket(port)
  private val pool = Executors.newFixedThreadPool(poolSize)

  private var closeRequested: Boolean = false

  def run() {
    try {
      while (!closeRequested) {
        // This will block until a connection comes in.
        try {
          val socket = serverSocket.accept()
          pool.execute{ () =>
            // read request
            val reader = new BufferedInputStream(socket.getInputStream)
            val inBytes = new Array[Byte](4096)
            val inSize = reader.read(inBytes)
            val request = new String(inBytes, 0, inSize)

            // respond
            val out = socket.getOutputStream
            val ans = request.toLowerCase match {
              case "ping" => "pong"
              case r => s"""unknown request "$r" """
            }

            out.write(ans.getBytes)
            out.close()
          }
        } catch {
          case _: SocketException if closeRequested => println(s"socket stopped on service port=$port")
        }
      }
    } finally {
      pool.shutdown()
    }
    println(s"service on port=$port stopped")
  }

  def close(): Unit = {
    closeRequested = true
    serverSocket.close()
    println(s"close service on port=$port")
  }
}
