package demo

import java.util.concurrent.TimeUnit

import org.apache.log4j.Logger

object MyAppLog extends App {
  private val log: Logger = Logger.getLogger(MyAppLog.getClass.getName)
  println("hello")
  log.info("scala info msg")
  log.debug("scala debug msg")
  val t:Thread = new Thread(new Runnable {
    override def run(): Unit = {
     TimeUnit.SECONDS.sleep(2)
      println("hello world in another thread ")}
  })
  t.start()
  t.join()

}
