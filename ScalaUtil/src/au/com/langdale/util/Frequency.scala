package au.com.langdale
package util

import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet
import Ordering._

class Frequency[A : Ordering,B : Ordering] extends HashMap[(A,B), Int] {

  var xs = new TreeSet[A]
  var ys = new TreeSet[B]
  
  def add(k1: A, k2:B, count:Int) {
    val key = (k1, k2)
    update(key, getOrElse(key, 0) + count)
    xs += k1
    ys += k2
  }
  
  def add(k1: A, k2:B) { add(k1, k2, 1) }
  
  def title = ""
  
  def printGrid {
    println(title)
    
    for( x <- xs ) print("\t" + x)  
    println
    
    for( y <- ys) {
      print(y)
      for( x <- xs) print("\t" + getOrElse((x,y), 0))
      println
    }
  }
}
