package org.jj.core

import java.io._
import java.net.Socket

case class SocketConnection(socket: Socket){

  class Reader(){
    private val reader = new BufferedInputStream(socket.getInputStream)
    def read(byteNumber: Int): Option[Array[Byte]] = {
      val inBytes = new Array[Byte](byteNumber)
      val inSize = reader.read(inBytes)
      if(inSize<1) None
      else Some(if(inSize < byteNumber) inBytes.take(inSize) else inBytes)
    }

    def readAll(): Option[Array[Byte]] = {
      var msg = List.empty[Array[Byte]]
      val bufSize = 4096
      var inSize = bufSize
      while(inSize==bufSize){
        var buffer = new Array[Byte](bufSize)
        inSize = reader.read(buffer)
        if(inSize>0)msg +:= buffer
      }

      if(msg.isEmpty) None
      else {
        val res = new Array[Byte]((msg.size-1) * bufSize + inSize)
        var pos = 0
        msg = msg.reverse
        while(msg.nonEmpty){
          val next = msg.head
          msg = msg.tail
          Array.copy(next, 0, res, pos, if(msg.isEmpty) inSize else bufSize)
          pos += bufSize
        }
        Some(res)
      }
    }
  }

  def reader(): Reader = new Reader()

  def readAll(): Option[Array[Byte]] = reader().readAll()

  def readString(): Option[String] = readAll().map(b => new String(b, 0, b.length))


  //
  // Output
  // ------

  def sendStr(msg: String): Unit = send(msg.getBytes)

  def send(bytes: Array[Byte]): Unit = {
    val output = socket.getOutputStream
    output.write(bytes)
    output.flush()
  }
}

trait SocketHelper{

  type MsgType1 = () => String

  protected def serialize[A](obj: A): Array[Byte] = {
    val bo = new ByteArrayOutputStream()
    new ObjectOutputStream(bo).writeObject(obj)
    bo.toByteArray
  }

  protected def deserialize[A](bytes:Array[Byte]): A = {
    new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject().asInstanceOf[A]
  }
}
