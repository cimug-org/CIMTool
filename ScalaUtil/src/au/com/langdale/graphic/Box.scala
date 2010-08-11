package au.com.langdale
package graphic

import scala.collection.mutable.{Buffer, ArrayBuffer, BufferProxy}
import Geometry._

abstract class Box extends Iterable[Rendition] {
  def area: Area
  def centroid: Centroid
  def +=( item: Rendition): Unit
  def ++=(items: Iterator[Rendition]) {
    for( item <- items) this += item
  }
  def split(quota:Int): Box
}

class FlatBox extends Box  {
  val self = new ArrayBuffer[Rendition]
  var area: Area = NullArea
  var centroid:Centroid = NullCentroid
  
  def iterator = self.iterator

  def +=(item: Rendition) {
    self += item
    centroid = item.shape.points.foldLeft(centroid)(_ include _)
    area = item.shape.points.foldLeft(area)( _ + _) 
    
  }
  
  def split(quota:Int) = {
    if( size <= quota )
      this
    else {
      val box = new QuadBox(area, centroid)
      box ++= self.elements
      box.split(quota)
    }
  }
}

class QuadBox(val area: Area, val centroid: Centroid) extends Box { 
  private val qs: Array[Box] = Array(new FlatBox, new FlatBox, new FlatBox, new FlatBox)

  def quadrants: Seq[Box] = qs
  
  override def size = qs.map( _.size ).reduceLeft( _ + _ )
  
  def iterator = qs.map( _.iterator ).reduceLeft( _ ++ _ )
  
  private def quadrant(p: Point):Int = 
    if(p.x >= centroid.x)
      if(p.y >= centroid.y) 0 else 3
    else
      if(p.y >= centroid.y) 1 else 2
    
  def +=( item: Rendition) {
    for( i <- 0 to 3; if item.shape.points.exists( quadrant(_) == i)) qs(i) += item
  }

  def split(quota:Int) = {
    for( i <- 0 to 3) qs(i) = qs(i).split(quota)
    this
  }
}
