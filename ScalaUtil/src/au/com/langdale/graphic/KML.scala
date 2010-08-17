package au.com.langdale
package graphic

import scala.xml.{Node, NodeSeq}
import scala.xml.NodeSeq.Empty
import scala.xml.XML.saveFull

import Geometry._

object KML extends KML {
  
}

trait KML {

  
  val coords: Point => LongLat = { case Point(x, y) => LongLat(x, y) }
  
  val stylePath = "styles.kml"
  
  def generate(box: Box, prefix: String) {
    for((inner, name) <- traverse(box, prefix))
      write(name)( renderArea(inner) ++ renderContent(inner))

    write(prefix + ".kml")( renderArea(box) ++ renderGrid(box, prefix) ++ renderLinks(box, prefix))
    write(prefix.substring(0,prefix.lastIndexOf('/')+1) + "styles")( renderStyles )
  }
  
  def traverse(box: Box, prefix:String): Iterable[(FlatBox, String)] = box match {
    case b: FlatBox =>
       Seq.singleton((b, prefix))
    case b: QuadBox =>
      for( i <- 0 to 3; 
           n <- traverse(b.quadrants(i), prefix + i)) yield n
  }
    
  def renderContent(box:FlatBox) =
    for( item <- box; 
         node <- renderFeature(item)) yield node
  
  def renderLinks(box:Box, prefix:String) = 
    for((inner, name) <- traverse(box, prefix); 
        node <- renderLink(inner, name)) yield node
  
  def renderGrid(box:Box, prefix:String) = 
    for((inner, name) <- traverse(box, prefix); 
        node <- renderRect(inner, name)) yield node
  
  def renderRect(box:Box, name:String) = box.area match { 
    case RectArea(p1, p2) => 
      <Placemark>
        <name>{name}</name>
        <styleUrl>{stylePath + "#box"}</styleUrl>
        { rectangle(coords(p1), coords(p2)) }
      </Placemark>
      
    case _ => Empty
  }
  
  def renderLink(box:FlatBox, prefix:String) = box.area match {
    case RectArea(p1, p2) => link(prefix, coords(p1), coords(p2))
    case _ => Empty
  }
  
  def renderArea(box:Box) = box.area match { 
    case RectArea(p1, p2) => region(coords(p1), coords(p2))
    case _ => Empty
  }
  
  def renderFeature(item: Rendition) = {
    val Rendition(name, style, shape) = item
    val styleRef = stylePath + "#" + style
    
    <Placemark>
      <name>{name}</name>
      <styleUrl>{styleRef}</styleUrl>
      { renderGeometry(shape) }  
    </Placemark>
  }
  
  def renderGeometry(shape: Shape): NodeSeq = renderGeometry(shape, 0.0)
  
  def renderGeometry(shape: Shape, altit: Double): NodeSeq = shape match {
    case OrientedPoint(point, _) => place(coords(point))
    case Segment(p1, p2) => path(List(coords(p1), coords(p2)), altit)
    case MultiSegment(points) => path(points.map(coords(_)), altit)
    case CenteredText(point, _, text) => place(coords(point))
    case Rect(p1, p2) => rectangle(coords(p1), coords(p2))
    case CompoundShape(shapes) =>
      <MultiGeometry>
      { shapes flatMap (renderGeometry(_, altit)) }
      </MultiGeometry>
      
    case _ => Empty
  }
  
  def renderStyles: NodeSeq = 
    style("line", "ffffffff", 3) ++
    icon_style("label", "ffffffff", 0.0)
  
  def write( name: String )( body: NodeSeq ) = 
    saveFull(name + ".kml",  kml( name, body ), true, null)
  
  def kml( name:String, body: NodeSeq) =
    <kml xmlns="http://www.opengis.net/kml/2.2">
      <Document>
        <name>{name}</name>
        { body }
      </Document>
    </kml>
    
  def place(pos: LongLat) =
      <Point>
        <coordinates>{pos.long},{pos.lat},0</coordinates>
      </Point>
  
  def path(points: Seq[LongLat], altit: Double) = 
      <LineString>
        {
          if(altit > 0.0) {
            <altitudeMode>relativeToGround</altitudeMode>
            // <extrude>1</extrude>
          }
          else
            <tessellate>1</tessellate>
        }
        <coordinates>
        {
          for( pos <- points) 
            yield pos.long + "," + pos.lat + "," + altit + " "
        }
        </coordinates>
      </LineString>
 
  def rectangle(l1: LongLat, l2:LongLat) =
      <LinearRing>
        <tessellate>1</tessellate>
        <coordinates>
          {l1.long},{l1.lat},0
          {l2.long},{l1.lat},0
          {l2.long},{l2.lat},0
          {l1.long},{l2.lat},0
          {l1.long},{l1.lat},0
        </coordinates>
      </LinearRing>

  def region(l1: LongLat, l2: LongLat) = 
    <Region>
      <LatLonAltBox>
        <north>{l2.lat}</north>
        <south>{l1.lat}</south>
        <east>{l2.long}</east>
        <west>{l1.long}</west>
      </LatLonAltBox>
      <Lod>
        <minLodPixels>400</minLodPixels>
      </Lod>
    </Region> 
  
  def link(name:String, l1: LongLat, l2: LongLat) =
    <NetworkLink>
      <name>{name}</name>
      {region(l1, l2)}
      <Link>
        <href>{name}.kml</href>
        <viewRefreshMode>onRegion</viewRefreshMode>
      </Link>
    </NetworkLink>
    
  def style(name:String, colour:String, width:Int) = 
    <Style id={name}>
      <LineStyle>
        <color>{colour}</color>
        <width>{width}</width>
        <colorMode>normal</colorMode>
      </LineStyle>
    </Style>
    
  def icon_style(name:String, colour:String, scale:Double) = 
    <Style id={name}>
      <LabelStyle>
        <color>{colour}</color>
      </LabelStyle>
   </Style>    
}

