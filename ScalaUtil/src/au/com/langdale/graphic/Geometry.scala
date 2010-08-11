package au.com.langdale
package graphic

import Math.{min, max, sqrt}
import scala.collection.mutable.ListBuffer

object Geometry {
  case class LongLat( long:Double, lat:Double)

  case class Point( x: Double, y: Double) {
    def +(p2:Point) = Point(x + p2.x, y + p2.y)
    def -(p2:Point) = Point(x - p2.x, y - p2.y)
    def dot(p2:Point) = x*p2.x + y*p2.y
    def *(p2:Point) = Point(x*p2.x - y*p2.y, x*p2.y + y*p2.x)
    def *(s:Double) = Point(x*s, y*s)
    def r = sqrt(x*x + y*y)
    def normalize = { val r0 = r; Point(x/r0, y/r0) }
  }

  object Origin extends Point(0.0, 0.0)

  def counterclock(points: List[Point]) = {
    // good for convex polygons:
    val p1 :: p2 :: p3 :: _ = points
    (p2.x - p1.x)*(p3.y - p2.y) - (p2.y - p1.y)*(p3.x - p2.x) >= 0.0
  }
  
  abstract class Area  {
    def contains(p: Point): Boolean
    def intersects(a: Area): Boolean
    def +(p: Point): Area 
    def +(a: Area): Area = a match {
      case RectArea(p1, p2) => this + p1 + p1
      case NullArea => this
    }
    def size: Double
  }

  object NullArea extends Area {
    def contains( p: Point ) = false
    def intersects(a: Area) = false
    def +(p: Point) = RectArea(p,p)
    def size = 0.0
  }

  case class RectArea( p1: Point, p2: Point) extends Area {
    def contains( p: Point ) = p.x >= p1.x && p.y >= p1.y && p.x <= p2.x && p.y <= p2.y
    def intersects(a: Area): Boolean = a match {
      case RectArea(q1, q2) => q2.x >= p1.x && q2.y >= p1.y && q1.x <= p2.x && q1.y <= p2.y 
      case NullArea => false
    }
    def +(p:Point) = RectArea(Point(min(p1.x, p.x), min(p1.y, p.y)), Point(max(p.x, p2.x), max(p.y, p2.y)))
    def toRect = Rect(p1, p2)
    def size = { val p = p2 - p1; p.x * p.y }
  } 

  class Centroid(private val point:Point, val weight:Double) extends Point(point.x, point.y) {
    def include(p:Point, w:Double):Centroid = {
      val wr = weight + w
      new Centroid(this * (weight/wr) + p * (w/wr), wr)
    }
    def include(p:Point):Centroid = include(p, 1.0)
    def include(c:Centroid):Centroid = include(c, c.weight)
  }

  object NullCentroid extends Centroid(Origin, 0.0)
  
  trait Coords extends ((Double, Double) => Point) {
    def unapply(p: Point): Option[(Double, Double)]
    val srid: Int
  }
  
  abstract class IdentityCoords extends Coords {
    def unapply(p: Point) = Some(p.x, p.y)
    def apply(x: Double, y: Double) = Point(x, y)
  }
  
  def transform(p: Point)(implicit coords: Coords): Point = coords(p.x, p.y)
  def invTransform(p: Point)(implicit coords: Coords): Point = p match { case coords(x, y) => Point(x, y) }

  def transform(p: OrientedPoint)(implicit coords: Coords): OrientedPoint = p match { 
    case OrientedPoint(p1, p2) => OrientedPoint(transform(p1), transform(p2))
  }
  def invTransform(p: OrientedPoint)(implicit coords: Coords): OrientedPoint = p match { 
    case OrientedPoint(p1, p2) => OrientedPoint(invTransform(p1), invTransform(p2))
  }
  
  object ISO216 {
    val A0 = Point(1189.0, 841.0)
  }
  
  trait Shape { def points: List[Point] }
  trait SimpleShape extends Shape 
  object NullShape extends SimpleShape { def points = Nil }

  case class Rect(p1: Point, p2: Point) extends SimpleShape { 
    def points = List(p1, p2)
    def normalize = Rect(Point(min(p1.x, p2.x), min(p1.y, p2.y)),Point(max(p1.x, p2.x), max(p1.y, p2.y)))
  }
  case class Circle(p1: Point, r: Double) extends SimpleShape { def points = List(p1) }
  case class Segment(p1: Point, p2: Point) extends SimpleShape { def points = List(p1, p2) }
  case class MultiSegment(points: List[Point]) extends SimpleShape
  case class Polygon(points: List[Point]) extends SimpleShape // implied segment from points.last to points.first. anticlockwise winding
  case class OrientedPoint(p1: Point, p2: Point) extends SimpleShape { def points = List(p1) }
  case class OrientedText(p: OrientedPoint, name: String, value: String) extends Shape { def points = List(p.p1) }
  case class CenteredText(p: Point, name: String, value: String) extends Shape { def points = List(p) }
 
  def OrientedPoint(p: Point): OrientedPoint = OrientedPoint(p, Point(1.0, 0.0) + p)
  def OrientedPoint(p: Point, v: Int): OrientedPoint = OrientedPoint(p, orient(v) + p)
  val orient = Array(Point(1.0,0.0), Point(0.0, 1.0), Point(-1.0, 0.0), Point(0.0, -1.0))
  def angle(op: OrientedPoint) = {
    val OrientedPoint(p1, p2) = op
    val Point(x, y) = (p2 - p1).normalize
    if( x > SIN45 )  0 else if( y > SIN45 ) 1 else if( x < -SIN45 ) 2 else 3  
  }
  val SIN45 = Math.sqrt(0.5)
  
  def TextLR(p: Point, value: String) = OrientedText( OrientedPoint(p), "label", value)
  def OrientedText(p: Point, v: Int, value: String): OrientedText = OrientedText( OrientedPoint(p, v), "label", value)
  
  case class CompoundShape(parts: List[SimpleShape]) extends Shape { def points = parts.flatMap( _.points )}
  def CompoundShape(shapes: Shape*): CompoundShape = {
    val parts = new ListBuffer[SimpleShape]
    for( s <- shapes ) s match {
      case CompoundShape(ps) => parts ++= ps
      case ss: SimpleShape => parts += ss
    }
    CompoundShape(parts.toList)
  }
  
  case class Rendition( name: String, style: String, shape: Shape)
  
  trait Renderable {
    def render: Rendition
  }
}
