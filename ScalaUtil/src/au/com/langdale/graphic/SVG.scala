package au.com.langdale
package graphic

import scala.xml.{Node, NodeSeq}
import scala.xml.NodeSeq.Empty
import scala.xml.XML.saveFull
import scala.collection.mutable.ListBuffer

import graphic.Geometry._

object SVG extends SVG 

trait SVG {

  def write( renditions: Iterable[Rendition], name: String ) { saveFull(name,  svg(renditions), true, null) }

  def svg( rs: Iterable[Rendition] ): Node =
    <svg width={width} height={height} viewBox={viewBox} version="1.1" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <style type="text/css">{ generalCSS }{ textCSS }</style>
        { supportingDefs }
      </defs>
      { elems(rs) }
    </svg>
  
  val width="1189mm" 
  val height="841mm" 
  val viewBox="0 0 1189 841"

  val generalCSS = ""
  def supportingDefs: NodeSeq = Empty  
  val textStyle = Map[String, String]()
  
  val textAlignType = Array( 
    "right"  ,
    "bottom", 
    "left"   ,
    "top" ,
    "center"
  ) 
  
  val textAlign = Array( 
    "text-anchor: start; alignment-baseline: middle;",
    "text-anchor: middle; alignment-baseline: top;",
    "text-anchor: end; alignment-baseline: middle;",
    "text-anchor: middle; alignment-baseline: bottom;",
    "text-anchor: middle; alignment-baseline: middle;"
  ) 
  
  val CENTERED = 4
  
  lazy val textCSS = {
    for( a <- 0 to 4; s <- textStyle.keySet ) yield List( "text.", textStyleName(s,a), " { ", textStyle(s), " ", textAlign(a), " }\n").mkString 
  }.mkString
  
  def textStyleName(s: String, a:Int) = List(s, "-", textAlignType(a)).mkString
  
  def elems(rs: Iterable[Rendition]): NodeSeq =
    for(Rendition(_, style, shape) <- rs.toSeq; e <- elems(shape, style)) yield e

  implicit def double2String( x: Double) = x.toString 
  
  def elems(s: Shape, style: String): NodeSeq = s match {
    case Segment(p1, p2) =>
      <line x1={p1.x} y1={p1.y} x2={p2.x} y2={p2.y} class={style}  />
    case Rect(p1, p2) =>
      val size = p2 - p1
      <rect x={p1.x} y={p1.y} width={size.x} height={size.y} class={style} />
    case Circle(c, r) =>
      <circle cx={c.x} cy={c.y} r={r} class={style} />  
    case OrientedText(op @ OrientedPoint(p1, _), _, value) =>
      val a = angle(op)
      val p = if( a == 1) p1 + Point(0.0, 4.0) else if( a == 0 || a == 2 )p1 + Point(0.0, 2.0) else p1
      <text x={p.x} y={p.y} class={textStyleName(style, a)}>{value}</text>
    case CenteredText(p, _, value) =>
      <text x={p.x} y={p.y} class={textStyleName(style, CENTERED)}>{value}</text>
    case CompoundShape(parts) =>
      for( p <- parts; e <- elems(p, style)) yield e
    case _ => Empty    
  }
}
