package concurrent

object NetApp {
  private val networkService = new NetworkService(6000,3)
  def main(args: Array[String]): Unit = {
    networkService.run()
  }
}
