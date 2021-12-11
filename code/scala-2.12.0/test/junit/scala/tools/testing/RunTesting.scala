package scala.tools.testing

import scala.reflect.runtime._
import scala.tools.reflect.ToolBox

trait RunTesting extends ClearAfterClass {
  def compilerArgs = "" // to be overridden
  val runner = cached("toolbox", () => Runner.make(compilerArgs))
}

class Runner(val toolBox: ToolBox[universe.type]) {
  def run[T](code: String): T = toolBox.eval(toolBox.parse(code)).asInstanceOf[T]
}

object Runner {
  def make(compilerArgs: String) = new Runner(universe.runtimeMirror(getClass.getClassLoader).mkToolBox(options = compilerArgs))
}
