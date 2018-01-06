package org.jj.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.Socket

trait Node{
  def ping(): Boolean
}

class LocalNode(socket: Socket) extends Node {

  val out = new BufferedOutputStream(socket.getOutputStream)
  val inp = new BufferedInputStream(socket.getInputStream)

  override def ping(): Boolean = {

    // send ping request
    out.write("ping".getBytes)
    out.flush()

    // read response


    //Il ne nous reste plus qu'à le lire

    var content = ""

    val inbytes = new Array[Byte](1024)
    var inSize = inp.read(inbytes)

    while (inSize != -1){
      content += new String(inbytes, 0, inSize)
      inSize = inp.read(inbytes)
    }

    content == "pong"
  }
}