package concurrent

import java.net.{ServerSocket, Socket}
import java.util.concurrent.{ExecutorService, Executors}

class NetworkService(val port:Int,val poolSize:Int) extends Runnable{

  val serverSocket = new ServerSocket(port)
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

  override def run(): Unit = {
    try {

      while (true) {
        val socket: Socket = serverSocket.accept()
        pool.execute(new Handler(socket))
      }
    }finally {
      pool.shutdown()
    }

  }
}
class Handler(socket: Socket)extends Runnable{
  def message =(Thread.currentThread().getName).getBytes
  override def run(): Unit = {
    socket.getOutputStream.write(message)
    socket.getOutputStream.close()
  }
}

