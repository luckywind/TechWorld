package base.japan

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-01-02  
 * @Desc
 */
object Demo {
  import javax.swing.{JFrame=>Window}
  import javax.swing.JFrame._
  val mameWindow = new Window("window 1")
  mameWindow setSize(200, 150)
  mameWindow setDefaultCloseOperation(EXIT_ON_CLOSE)
  mameWindow setVisible(true)

  def main(args: Array[String]): Unit = {
    Demo
  }
}
