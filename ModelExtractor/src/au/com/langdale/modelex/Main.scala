package au.com.langdale
package modelex
import db._
import Oracle._
import DBA._
import ResultExtractor._
import java.sql.ResultSet
import SQLQuoter._
import util.CommandLine.parse
import java.io.{OutputStreamWriter, FileWriter}
import scala.xml._

object Main {
  import Dot._
  
  type MMap[A,B] = Map[A,Set[B]]
  def multimap[A,B]( abs: Traversable[(A, B)]): MMap[A, B] = abs.groupBy { _._1 } mapValues ( _ map { _._2 } toSet )
  def multimap[A,B]( abs: Iterator[(A, B)]): MMap[A, B] = multimap(abs.toTraversable)  
  def invert[A,B]( abs: Map[A,B]): MMap[B, A] = multimap(abs.toSeq.map { _.swap })

  def main(argv: Array[String]) {
    
    val (args, opts) = parse(argv)
    
    val schema = opts get "schema" getOrElse "EAPM"
    
    val spec = OracleSpec(
        opts get "host" getOrElse "petrel", 
        opts get "sid" getOrElse "orcl", 
        schema, 
        opts get "password" getOrElse "memory" )
    
    setupDriver

    val result = withConnection(spec) { db =>
    
      val tables = 
        multimap(
          for(CO(table, column, dtype) <- db query CO.all) 
            yield (SO(schema, table), (column, dtype)))
      
      val pks = 
        (for( PK(id, table) <- db query PK.all ) 
          yield (id, table)).toMap
      
      val fks = 
        (for( FK(id, table, to) <- db query FK.all) 
          yield (id, (table, to))).toMap
          
      val keyCols = 
        multimap(
          for( CC(id, table, column) <- db query CC.all if (pks contains id) || (fks contains id)) 
            yield ((table, column), id))
      
      val connected = 
        (for((tablea, to) <- fks.values; table <- List(tablea, pks(to))) 
          yield table).toSet.toList //sortBy {(table: SO) => table.name}
          
      val table2pks = invert(pks)
      
      val table2fks = 
        multimap(
          for((id, (table, _)) <- fks.toSeq) 
            yield (table, id))
            
      val cons2cols = 
        multimap(
          for(((table, column), ids) <- keyCols.toSeq; id <- ids) 
            yield (id, (table, column)))
            
      val cols2cons = invert(cons2cols)
      
      def single(fk: SO) = cols2cons(cons2cols(fk)) exists pks.contains
      
      def isKeyCol(table: SO, column: String) =
        keyCols.getOrElse((table, column), Set.empty) exists pks.contains
      
      //println(cons2cols(SO("EAPM","PK_PM_PT3WDG")))
      
      def simpleDot = {
        def splitCols(table: SO) = {
          val (ab, c) = tables(table).toList.unzip._1.sorted partition { column => keyCols contains (table, column) }
          val (a, b)  = ab partition { column => keyCols((table, column)) exists pks.contains }
          List(a, b, c)
        }
        
        def tableboxes =
          for(table <- connected; cols = splitCols(table)) 
            yield box(Some(ident(table)), table.name :: cols)
        
        def edges =
          for((fk, (tablea, pk)) <- fks.toList; tableb = pks(pk)) 
            yield edgeStyled(ident(tablea), ident(tableb), single(fk))
        
        digraph( tableboxes, edges)
      }
      
      def complexDot = {
        def consboxes(table: SO, table2cons: MMap[SO, SO]) = 
          for( cons <- table2cons.getOrElse(table, Set.empty).toList sortBy (_.name)) 
            yield box(Some(cons.name), cons2cols(cons).toList.unzip._2.sorted)
        
        def attrbox(table: SO) = 
          box(None, 
            (for((column, dtype) <- tables(table) if ! keyCols.contains((table,column))) 
              yield column+" : "+dtype).toList.sorted)

        def tableboxes = 
          for( table <- connected ) 
            yield box(Some(ident(table)), 
              List(table.name) ++ consboxes(table, table2pks) ++ consboxes(table, table2fks) ++ List(attrbox(table)))
        
        def edges = 
          for((fk, (tablea, pk)) <- fks.toList; tableb = pks(pk)) 
            yield edgeStyled(portid(tablea, fk.name), portid(tableb, pk.name), single(fk))
        
        digraph(tableboxes, edges)
      }
      
      def csv = {
        def fkOpts( table: SO, column: String) =
          keyCols get (table, column) map 
            (for( cons <- _ if fks contains cons) yield Some(cons): Option[SO]) getOrElse 
              Set(None: Option[SO])
        
        def refInfo(table: SO) = 
          for((column, dtype) <- tables(table); fkOpt <- fkOpts(table, column)) 
            yield Info(table, column, dtype, isKeyCol(table, column), fkOpt map (fk => pks(fks(fk)._2)))
        
        for(table <- connected sortBy (_.name); 
          info <- refInfo(table).toList sortBy (_.sortKey)) 
            yield info
      }
      
      opts get "format" match {
        case Some("simpledot") => simpleDot.repr
        case Some("complexdot") => complexDot.repr
        case Some("csv") => csv.mkString("\n")
        case None => simpleDot.repr
      }
    }
    val out = opts get "output" map { name => new FileWriter(name)} getOrElse new OutputStreamWriter(System.out)
    out.write(result)
    out.close
    
  }
  
  def edgeStyled( id1: String, id2: String, manyValued: Boolean) = 
    if(manyValued) 
      edge(id1, id2, "color" -> "red", "arrowhead" -> "empty" )
    else
      edge(id1, id2, "color" -> "blue", "arrowhead" -> "open" )
      
  def ident( value: String): String = value replace("$", "_")
  def ident( value: SO ): String = ident(value.schema + "_" + value.name)
  def portid( node: SO, port: String ): String = ident(node) + ":" + ident(port)
}

object Dot {
  def escape(text: String) = text.replace("\"", "\\\"").replace("\\", "\\\\")
  def quote(item: Any) = item match {
    case html: NodeSeq => "\n<" + html.toString + ">\n"
    case text => "\"" + escape(text.toString) + "\""
  }

  trait Lang { def repr: String }
  
  trait Box extends Lang {
    val id: Option[String]
    def html( level: Int ): NodeSeq
    def text: String
    def repr = node(id.get, "shape" -> "plaintext", "label" -> html(0)).repr
  }
  
  case class MultiBox( id: Option[String], parts: List[Box]) extends Box {
    def text = parts map (_.text) mkString("{", "|", "}")
    
    def html( level: Int ): NodeSeq = 
      if( !parts.isEmpty)
        <table border="1" cellborder="0">
        { 
          for( part <- parts ) yield part.id match {
            case Some(port) => <tr><td port={ port }>{part.html(level+1)}</td></tr> 
            case _ => <tr><td>{part.html(level+1)}</td></tr> 
          }
        }
        </table>
      else
        NodeSeq.Empty
  }
  
  case class SingleBox( id: Option[String], part: String ) extends Box {
    def text = part

    def html( level: Int ): NodeSeq = 
      if( level > 0)
        Text(part)
      else
        <table><tr><td>{part}</td></tr></table>
  }
  
  def box( id: Option[String], parts: List[Any]): Box = {
    // println("box("+id+", "+parts+")")
    MultiBox(id, parts.map {
      case b: Box => b
      case s: String => SingleBox(None, s)
      case s :Seq[Any] => box( None, s.toList)
      case a => SingleBox(None, a.toString)
    })
  }
  type Attrib = (String, Any)

  case class Attribs(values: List[Attrib]) extends Lang {
    def repr = values.map { case (key, value) => key + "=" + quote(value) } .mkString("[", ",", "]")
  }
  
  case class Node( id: String, attribs:Attribs) extends Lang {
    def repr = id + " " + attribs.repr
  }
  
  def node( id: String, attribs: Attrib*) = Node(id, Attribs(attribs.toList))
  
  case class Edge( id1: String, id2: String, attribs: Attribs) extends Lang {
    def repr = "%s -> %s %s".format(id1, id2, attribs.repr)
  }
  
  def edge( id1: String, id2: String, attribs: Attrib*) = Edge( id1, id2, Attribs(attribs.toList))
  
  case class Digraph(items: List[Lang]) extends Lang {
    def repr = "digraph {\n  %s\n}".format(items map (_.repr) mkString "\n  ")
  }
  
  def digraph( groups: List[Lang]*) = Digraph(groups.toList.flatten)
}

case class SO( schema: String, name: String )
case class FK( id: SO, table: SO, to: SO )
case class PK( id: SO, table: SO )
case class CC( id: SO, table: SO, column: String )
case class CO( table: String, column: String, dtype: String)

case class Info(table: SO, column: String, dtype: String, isKey: Boolean, ref: Option[SO]) {
  private def indicator = if(isKey) "*" else ""
  private def toList( id: SO) = List(id.schema, id.name) 
  override def toString = toList(table) ++ List(column, dtype, indicator) ++ toList(ref getOrElse SO("", "")) mkString ","
  def sortKey = (!isKey, !ref.isDefined, column)
}

object FK extends RecordType[FK] {
  def apply(implicit rs: ResultSet) = FK(SO('OWNER, 'CONSTRAINT_NAME), SO('OWNER, 'TABLE_NAME), SO('R_OWNER, 'R_CONSTRAINT_NAME))
  def all = this query 	SELECT( 'OWNER, 'CONSTRAINT_NAME, 'TABLE_NAME, 'R_OWNER, 'R_CONSTRAINT_NAME) ~ 
            FROM(ID('SYS, 'USER_CONSTRAINTS)) ~ WHERE( 'CONSTRAINT_TYPE ~ EQ ~ "R" ~ AND ~ 'TABLE_NAME ~ NOT ~ LIKE ~ "%$%")
}

object PK extends RecordType[PK] {
  def apply(implicit rs: ResultSet) = PK(SO('OWNER, 'CONSTRAINT_NAME), SO('OWNER, 'TABLE_NAME))
  def all = this query  SELECT( 'OWNER, 'CONSTRAINT_NAME, 'TABLE_NAME) ~ 
            FROM(ID('SYS, 'USER_CONSTRAINTS)) ~ WHERE( 'CONSTRAINT_TYPE ~ IN( "P", "U")  ~ AND ~ 'TABLE_NAME ~ NOT ~ LIKE ~ "%$%")
  
}

object CC extends RecordType[CC] {
  def apply(implicit rs: ResultSet) = CC(SO('OWNER, 'CONSTRAINT_NAME), SO('OWNER, 'TABLE_NAME), 'COLUMN_NAME)
  def all = this query  SELECT( 'OWNER, 'CONSTRAINT_NAME, 'TABLE_NAME, 'COLUMN_NAME) ~ 
            FROM(ID('SYS, 'USER_CONS_COLUMNS)) ~ WHERE ( 'TABLE_NAME ~ NOT ~ LIKE ~ "%$%") // ~ ORDER ~ BY( 'OWNER, 'CONSTRAINT_NAME )
}

object CO extends RecordType[CO] {
  def apply(implicit rs: ResultSet) = CO('TABLE_NAME, 'COLUMN_NAME, 'DATA_TYPE)
  def all = this query SELECT('TABLE_NAME, 'COLUMN_NAME, 'DATA_TYPE) ~ FROM(ID('SYS, 'USER_TAB_COLUMNS)) ~ WHERE ( 'TABLE_NAME ~ NOT ~ LIKE ~ "%$%")
}
