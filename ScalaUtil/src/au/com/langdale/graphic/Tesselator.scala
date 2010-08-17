package au.com.langdale
package graphic
import Tesselator._

import graphic.Geometry._
import Math._
import scala.collection.mutable.{ListBuffer, HashMap, HashSet}

object Tesselator {
  case class Cell( x: Int, y: Int )
  class Tile[A](val item: A)
  
  def floorCell(p: Point): Cell = Cell(floor(p.x).toInt, floor(p.y).toInt)
  def scale(p1: Point, p2:Point) = Point(p1.x*p2.x, p1.y*p2.y)
  
}

class Tessellator[A](val cellSize: Point) {
  val r = Point( 1.0/cellSize.x, 1.0/cellSize.y)
  val tiles = new HashMap[Cell, ListBuffer[Tile[A]]]

  def size = tiles.size
  var count = 0
    
  def tessellate(area: Area): Collection[Cell] = area match {
    case RectArea(p1, p2) =>
      val u1 = floorCell(scale(p1, r))
      val u2 = floorCell(scale(p2, r))

      for( x <- u1.x to u2.x; y <- u1.y to u2.y) yield Cell(x, y)

    case _ => Seq.empty   
  }
    
  def apply(area: Area): Iterable[A] = apply(tessellate(area))
      
  def apply(cells: Collection[Cell]): Iterable[A] = {  
    val buf = new HashSet[Tile[A]]
      
    for {
      u <- cells
      bs <- tiles.get(u)
      b <- bs
    } 
      buf += b
      
    Log(" window/total cells " + cells.size + "/" + size + " paths " + buf.size + "/" + count)
      
    for( b <- buf ) yield b.item
  }
    
  def update(area: Area, item: A) {
    val b = new Tile(item)
    for( u <- tessellate(area)) {
      val buf = tiles.getOrElseUpdate( u, new ListBuffer[Tile[A]])
      buf += b
    }
    count += 1
  }
    
  def all: Iterable[A] = {
    val buf = new HashSet[Tile[A]]
    for( bs <- tiles.values; b <- bs ) buf += b
    for( b <- buf ) yield b.item
  }
    
  def clear { tiles.clear }
}
