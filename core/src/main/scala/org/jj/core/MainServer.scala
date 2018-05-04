package org.jj.core

import java.io.{File, FileOutputStream, PrintStream}

object MainServer {

  def main(args: Array[String]): Unit = {
    val port = if(args.nonEmpty) args(0).toInt else 13013
    val filename = s"out-$port.txt"
    //redirectOutput(filename)
    println(s"App starting with port $port (out = $filename)")

    val server = new MainServer(port, 1)

    Runtime.getRuntime.addShutdownHook(new Thread{
      override def run(): Unit = {
        println("closing on shutdown signal")
        server.stop()
      }
    })

    server.start()
  }

  def redirectOutput(filename: String): Unit = {
    val file = new File(filename)
    val fos = new FileOutputStream(file, true)
    val ps = new PrintStream(fos)
    System.setOut(ps)
  }

}

case class MainServer(port: Int, poolSize: Int) extends Server {

  override def answer(connection: SocketConnection): Option[String] = connection.readAll() match {
      case Some(requestBytes) =>
        requestBytes(0) match {
          case 1 =>
            val f = deserialize[MsgType1](requestBytes.tail)
            connection.sendStr("received ()=> " + f())
            None

          case 2 =>
            val a = deserialize[AppTest](requestBytes.tail)
            connection.sendStr("received " + a.message())
            None

          case _ =>
            val request = new String(requestBytes, 0, requestBytes.length)
            request.trim.toLowerCase match {
              case "ping" => connection.sendStr("pong"); None
              case "bye"  => connection.sendStr("bye"); Some("on client request")
              case r      => connection.sendStr(s"""unknown request "$r" """); None
            }
        }

      case None =>
        Some("- client has stop connection abruptly")
    }
}