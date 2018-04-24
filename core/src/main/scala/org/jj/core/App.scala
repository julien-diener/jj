package org.jj.core

import java.io.File
import java.nio.file.Files

import scala.util.{Failure, Success, Try}

/** to be executed by Server */
trait App {


}

object AppUtils {
  /** return path to this class jar file
    *
    * If this class file is not in a jar, make the jar
    */
  def getJarFile(cls: Class[_], deleteOnExit: Boolean = true): Try[File] = for {
    simpleName <- try {
      Success(cls.getSimpleName)
    } catch {
      case _: InternalError => Failure(nestedClass(cls))
    }

    classFile = cls.getResource(cls.getSimpleName + ".class")
    file = cls.getProtectionDomain.getCodeSource.getLocation.getFile

    jar <-
      // jar file found
      if(file.endsWith(".jar")) Success(file)

      else if (classFile == null) Failure(nestedClass(cls))

      // case where file is not in a jar
      else {
        cls.getName.split('.').headOption match {
          case None =>
            Failure(nestedClass(cls))

          case Some(firstDir) =>
            //  jar is the directory in the classpath
            //  zip first dir (given by object fqcn) in a jar
            val tmpDir = Files.createTempDirectory("jj.liveJar")
            println(cls.getName)
            val liveJar = tmpDir.resolve(s"${cls.getName}.jj.live.jar").toFile
            if(deleteOnExit){
              tmpDir.toFile.deleteOnExit()
              liveJar.deleteOnExit()
            }
            zip(liveJar, file, firstDir)
            Success(liveJar.getAbsolutePath)
        }
      }
  } yield new File(jar)

  private def nestedClass(cls: Class[_]) = AppException(s"Invalid App subclass '${cls.getName}' - probably a nested class", None)

  private def zip(out: File, baseDir: String, dir: String): Unit = {
    import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
    import java.util.zip.{ZipEntry, ZipOutputStream}

    def parseFiles(d: File)(f: (File) => Unit): Unit = {
      val (subDirs, files) = d.listFiles().partition(_.isDirectory)
      files.foreach(f)
      subDirs.foreach(sd => parseFiles(sd)(f))
    }

    val zip = new ZipOutputStream(new FileOutputStream(out))

    val basePath = new File(baseDir).toPath
    parseFiles(new File(baseDir, dir)){ fullName =>
      val name = basePath.relativize(fullName.toPath)
      zip.putNextEntry(new ZipEntry(name.toString))
      val in = new BufferedInputStream(new FileInputStream(fullName))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }
    zip.close()
  }
}

case class AppException(msg: String, cause: Option[Throwable]) extends Exception(msg, cause.orNull)