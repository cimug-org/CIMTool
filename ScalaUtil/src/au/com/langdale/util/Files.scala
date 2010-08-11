package au.com.langdale
package util

import java.io._
import scala.io.Source

object Files {
  
  def file( path: String) = new File(path)
  def basename(path: String) = path.substring(path.lastIndexOf('/')+1)
  def stripext(path: String) = if(basename(path).contains('.')) path.substring(0, path.lastIndexOf('.')) else path
  def defaultext(path: String, ext: String) = if( basename(path) contains "." ) path else path + "." + ext
  
  def ensureDir(e: File) {
    if( e.exists ) 
      require( e.isDirectory )
    else
      e.mkdirs
  }
  
  def copyDir(d: File, e: File) {
    ensureDir(e)
    for( f <- d.listFiles; g = new File(e, f.getName))
      copy(f, g)
  }
  
  def copyFile( a:File, b:File) {
    println(a +" -> " + b)
    val ac = new FileInputStream(a).getChannel
    val bc = new FileOutputStream(b).getChannel
    ac.transferTo(0, ac.size, bc)
  }
  
  def copy(a:File, b:File) {
    if( a.isDirectory)
      copyDir(a,b)
    else
      copyFile(a,b)
  }
  
  class RichFile(f: File) {
    def /(n: String) = new File(f, n)
    def copyTo( g: File) {copy(f,g)}
    def open = Source.fromFile(f)
    def lines = open.getLines("\r\n")
    def out = new PrintStream( new BufferedOutputStream ( new FileOutputStream(f)))
  }
  
  implicit def RichFile(f: File) = new RichFile(f)

}
