package org.jj.core

import java.net.{ConnectException, Socket}

object AppTest {

  def main(args: Array[String]): Unit = {
    runLocal(2000, 5){
      nodes =>
        nodes.foreach {
          node => println(s"ping: " + node.ping())
        }
    }
  }

  def runLocal(startPort: Int, n: Int)(f: Seq[Node] => Unit): Unit = {

    val fqcn = "org.jj.core.App"
    val classpath = System.getProperty("java.class.path")//.split(File.pathSeparator)
    val rt = Runtime.getRuntime

    val apps = new Array[Process](n)

    try{
      (0 until n).foreach{ i =>
        val port = 2001 + i
        val app = rt.exec(s"java -classpath $classpath $fqcn $port")
        apps(i) = app

        val pid = if (app.getClass.getName.equals("java.lang.UNIXProcess")) {
          val f = app.getClass.getDeclaredField("pid")
          f.setAccessible(true)
          "pid=" + f.getLong(app)
        } else {
          app.toString
        }
        println(s"running app $pid receiving on port $port")
      }

      // connect to deployed app
      val nodes = new Array[Node](n)
      var connected = 0
      var iter = 0
      while(connected < n && iter < 100){
        Thread.sleep(50)
        (0 until n).foreach { i =>
          if(nodes(i) == null) {
            val port = 2001 + i
            try {
              val socket = new Socket("127.0.0.1", port)
              nodes(i) = new LocalNode(socket)
              connected += 1
            } catch {
              case _: ConnectException => //println(s" *** ConnectException on port $i ***")
            }
          }
        }
        iter += 1
      }
      if(connected < n){
        throw new RuntimeException(s"could not connect to all apps ($connected/$n)")
      }


      f(nodes)

    } finally {
      apps.foreach{
        case null => println("no app to kill")
        case app => app.destroyForcibly()
      }
    }
  }

  def runThread(startPort: Int, n: Int)(f: Seq[Node] => Unit): Unit = {

    val netSrv = new Array[NetworkService](n)
    val threads = new Array[Thread](n)
    val nodes = new Array[Node](n)

    try{
      (0 until n).foreach{ i =>
        val port = 2001 + i
        val srv = new NetworkService(port, 1)
        netSrv(i) = srv

        val thread = new Thread(srv)
        thread.start()
        threads(i) = thread

        val tid = thread.getId
        println(s"running thread $tid receiving on port $port")

        //Thread.sleep(100)
        nodes(i) = new LocalNode(new Socket("127.0.0.1", port))
      }

      f(nodes)

      Thread.sleep(2000)

    } finally {
      (0 until n).foreach{ i =>
        netSrv(i).close()
        threads(i).interrupt()
        threads(i).stop()
        threads(i) = null
      }
    }
  }
}
