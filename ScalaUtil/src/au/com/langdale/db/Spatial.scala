package au.com.langdale
package db
import java.sql.{Connection, ResultSet}
import oracle.spatial.geometry.JGeometry
import oracle.sql.STRUCT
import graphic.Geometry._
import scala.collection.mutable.ListBuffer
import Math._

object Spatial {
  def shapeToStruct(s: Shape)(implicit coords: Coords, db: Connection): STRUCT = shapeToGeom(s) map (JGeometry.store(_, db)) getOrElse null
  
  def shapeToGeom(s: Shape)(implicit coords: Coords): Option[JGeometry] = shapeToOrds(s) map {
    case (gtype, info, ords) => new JGeometry(gtype, coords.srid, info.toArray, ords.toArray)
  }
  
  def shapeToOrds(s: Shape)(implicit coords: Coords): Option[(Int, List[Int], List[Double])] = s match {
    case OrientedPoint( coords(x1, y1), coords(x2, y2)) =>
      val v = Point(x2-x1, y2-y1).normalize
      Some(2001, List(1, 1, 1, 3, 1, 0), List(x1, y1, v.x, v.y))
     
    case Segment(coords(x1, y1), coords(x2, y2)) =>
      Some(2002, List(1, 2, 1), List(x1, y1, x2, y2))
    
    case MultiSegment(points) =>
      Some(2002, List(1, 2, 1), points.flatMap { case coords(x,y) => List(x, y) })
    
    case Polygon(points) =>
      val ps1 = points map { case coords(x, y) => Point(x, y) }
      val ps2 = if( counterclock(ps1)) ps1 else ps1.reverse
      Some(2003, List(1, 1003, 1), (points.last :: points).flatMap { case Point(x,y) => List(x, y) })
      
    case Rect( coords(x1, y1), coords(x2, y2)) => 
      Some(2003, List(1, 1003, 3), List(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2)))
      
    case Circle( coords(xc, yc), r) =>
      Some(2003, List(1, 1003, 4), List(xc +r, yc, xc, yc+r, xc-r, yc))
      
    case CompoundShape(parts) =>
      val info = new ListBuffer[Int]
      val ords = new ListBuffer[Double]
      var offset = 1
      
      for( p <- parts) shapeToOrds(p) match {
        case Some((_, List(_, e, i), o)) =>
          info ++= List(offset, e, i)
          ords ++= o
          offset += o.length
          
        case None =>
      }
      Some(2004, info.toList, ords.toList)
      
    case _ => None
  }
  
  def structToShape( struct: AnyRef)(implicit transform: (Double, Double) => Point) = geomToShape(JGeometry.load(struct.asInstanceOf[STRUCT]))
  
  def geomToShape(geom: JGeometry)(implicit transform: (Double, Double) => Point): Shape = ordsToShape(geom.getElemInfo, geom.getOrdinatesArray) 
  
  def ordsToShape(info: Array[Int], ords: Array[Double])(implicit transform: (Double, Double) => Point): Shape = (info, ords) match {
    case (Array(1, 1, 1, 3, 1, 0), Array(x1, y1, x2, y2))
        => OrientedPoint(transform(x1, y1), transform(x2+x1, y2+y1))
         
    case (Array(1, 2, 1), Array(x1, y1, x2, y2))
        => Segment(transform(x1, y1), transform(x2, y2))
         
    case (Array(1, 1003, 3), Array(x1, y1, x2, y2))
        => Rect(transform(x1, y1), transform(x2, y2)).normalize
         
    case (Array(1, 1003, 4), Array(x1, y1, x2, y2, x3, y3)) 
        // if y1 == y3 && 2*x2 == x1 + y1 && x3 - x1 == 2*(y2 - y1)
        => Circle(transform(x2, y1), y2-y1)
        
    case (Array(1, 2, 1), ords)
        => MultiSegment(0 until ords.length by 2 map {i => transform(ords(i), ords(i+1))} toList)
        
    case (Array(1, 1003, 1), ords)
        => Polygon(2 until ords.length by 2 map {i => transform(ords(i), ords(i+1))} toList)
        
    case (info, ords) if info.length > 3 && info.length % 3 == 0 => 
      val shapes = for { 
        ix <- 3 to info.length by 3
        subinfo = info.slice(ix-3, ix)
        ord1 = info(ix-3)-1
        ord2 = if(ix == info.length) ords.length else info(ix)-1 
        if ord1 >= 0 && ord2 > ord1 && ords.length >= ord2
        subords = ords.slice(ord1, ord2)
      } yield ordsToShape(Array(1, subinfo(1), subinfo(2)), subords).asInstanceOf[SimpleShape]
      
      CompoundShape(shapes.toList)
        
    case _ => NullShape
  }
  

}
