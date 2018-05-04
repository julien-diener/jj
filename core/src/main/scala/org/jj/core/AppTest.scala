package org.jj.core

import java.net.ConnectException

class AppTest(content: String) extends Serializable {
  def message(): String = content
}

object App1 extends App{ def start(port: Int): Int = 0 }
class App2() extends App{ def start(port: Int): Int = 0 }

object AppTest extends SocketHelper {

  class App3() extends App{ def start(port: Int): Int = 0 }

  def main(args: Array[String]): Unit = {

    class App4() extends App{ def start(port: Int): Int = 0 }
    val app5 = new App{ def start(port: Int): Int = 0 }

    println(AppUtils.getJarFile(App1.getClass))
    println(AppUtils.getJarFile(new App2().getClass))
    println(AppUtils.getJarFile(new App3().getClass))
    println(AppUtils.getJarFile(new App4().getClass))
    println(AppUtils.getJarFile(app5.getClass))

    val node = new ClientNode("127.0.0.1", 13013)
    println("ping: " + node.ping())

    val msg = new AppTest("Hello World")
    val response = node.sendMsg2(msg)

    println(response)
  }

  def main0(args: Array[String]): Unit = {
    Thread.sleep(2000)
    runLocal(2000, 2){
      nodes =>
        nodes.foreach {
          node =>
            Thread.sleep(100)
            println(s"client *** ping1: " + node.ping())
            Thread.sleep(100)
            println(s"client *** ping2: " + node.ping())
        }
        Thread.sleep(2000)
        nodes.foreach {
          node => println(s"bye: " + node.bye())
        }
    }
    println(" *** END ***")
  }

  def runLocal(startPort: Int, n: Int)(f: Seq[ClientNode] => Unit): Unit = {

    val fqcn = classOf[Server].getName
    val classpath = System.getProperty("java.class.path")
    val rt = Runtime.getRuntime

    val apps = new Array[Process](n)

    try{
      (0 until n).foreach{ i =>
        val port = 2001 + i
        val cmd = s"java -classpath $classpath $fqcn $port"
        println(s"running cmd '$cmd'")
        val app = rt.exec(cmd)
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
      val nodes = new Array[ClientNode](n)
      var connected = 0
      var iter = 0
      while(connected < n && iter < 10){
        Thread.sleep(200)
        (0 until n).foreach { i =>
          if(nodes(i) == null) {
            val port = 2001 + i
            try {
              nodes(i) = new ClientNode("127.0.0.1", port)
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

  def runThread(startPort: Int, n: Int)(f: Seq[ClientNode] => Unit): Unit = {

    val netSrv = new Array[Server](n)
    val threads = new Array[Thread](n)
    val nodes = new Array[ClientNode](n)

    try{
      (0 until n).foreach{ i =>
        val port = 2001 + i
        val srv = new MainServer(port, 5)
        netSrv(i) = srv

        val thread = new Thread(srv)
        thread.start()
        threads(i) = thread

        val tid = thread.getId
        println(s"client running thread $tid receiving on port $port")

        //Thread.sleep(100)
        nodes(i) = new ClientNode("127.0.0.1", port)
      }

      println("client call F")
      f(nodes)
      println("client F done")

      Thread.sleep(2000)

    } finally {
      (0 until n).foreach{ i =>
        val srv = netSrv(i)
        if(srv != null) srv.stop()
        //threads(i).interrupt()
        threads(i) = null
      }
    }
  }
}
