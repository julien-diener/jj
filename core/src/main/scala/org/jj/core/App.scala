package org.jj.core

import java.io.File

/** to be executed by Server */
trait App {

  def jarFile(): File = {
    println(this.getClass.getSimpleName)
    println(this.getClass.getResource(this.getClass.getSimpleName + ".class"))
    println(this.getClass.getResource(this.getClass.getSimpleName + ".class").getFile)

    val importPath = new File(this.getClass.getProtectionDomain.getCodeSource.getLocation.getFile)
    println(importPath.getAbsolutePath)
    importPath
  }

}
