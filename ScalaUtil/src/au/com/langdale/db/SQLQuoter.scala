package au.com.langdale
package db

import scala.collection.immutable.ListMap
import graphic.Geometry._
import java.util.Date
import java.sql.{Timestamp, ResultSet}
import DBA.{SQL, quote, Query}

object SQLQuoter {
  
  trait View {
    val name: Symbol
    val columns: List[(Symbol, SQL)]
    def toTable: Table
    def columnNames = columns map (_._1)
    def headerSQL: SQL = name((columnNames map ID): _*)
    def toSelectAllSQL = SELECT(columnNames.map(x => ID(x)):_*) ~ FROM( name )
  }
  
  case class Table(name: Symbol, columns: List[(Symbol, SQL)]) extends View {
    def keyColumnName = columns.head._1
    def apply(values: SQL*) = TableRow(name, columnNames zip values.toList)
    def toCreateSQL: SQL = CREATE ~ TABLE ~ name(columns.map(toColumnSQL(_)):_*)
    def toDiffTable = Table(Symbol(name.name + "_DIFF"), columns ++ List('WM_DIFFVER -> VARCHAR2(256), 'WM_CODE -> VARCHAR2(2)))
    def toGenericQuery(pred: SQL) = new Query[Map[Symbol,AnyRef]] {
      val sql: SQL = toSelectAllSQL ~ pred
      def fetch( implicit results: ResultSet ): Map[Symbol,AnyRef] = 
        Map() ++ { columnNames map (x => x -> results.getObject(x.name)) filter (_._2 != null)}
    }
    def toDeleteAllSQL = DELETE ~ FROM ~ name
    def toCopyAllSQL( other: Table) = INSERT ~ INTO ~ headerSQL ~ other.toSelectAllSQL 
    def toTable = this
    def toInsertTemplate = INSERT ~ INTO ~ headerSQL ~ VALUES((columnNames map (n => P)): _*)
  }
  
  def Table(name: Symbol, columns: (Symbol, SQL)*): Table = Table(name, columns.toList) 

  case class Layer(name: Symbol, left: View, right: View, join: (Symbol, Symbol), filters: (Symbol, SQL)*) extends View {
    val columns = columnMap.toList
    
    def columnMap = ListMap({
      for { 
        t <- List(left, right)
        c @ (n, _) <- t.columns 
        // if ! (filters map (_._1) contains n) 
        if (n != join._2 || join._1 == join._2)
      } yield c
    }: _*)
  
    def toTable = Table(name, columns)
  
    def toJoinSQL = 
      SELECT( List(ID(left.name.name, join._1.name)) ::: columnNames.filter( _ != join._1).map(x=>ID(x)) : _*) ~
      FROM ~ left.name ~ INNER ~ JOIN ~ right.name ~ 
      ON ~ ID(left.name, join._1) ~ EQ ~ ID(right.name, join._2) ~
      { filters map { case (name, value) => AND ~ name ~ EQ ~ value }}.reduceLeft(_ ~ _)
  }
  
  case class TableRow(name: Symbol, fields: List[(Symbol, SQL)]) {
    def columnNames = fields map (_._1)
    def values = fields map (_._2)
    def toInsertSQL: SQL = INSERT ~ INTO ~ name((columnNames map ID):_*) ~ VALUES(values:_*)
    def toDeleteSQL: SQL = DELETE ~ FROM ~ name ~ WHERE ~ toEqualsSQL(fields.head)
    def toUpdateSQL: SQL = UPDATE ~ name ~ SET((fields.tail map toEqualsSQL): _*) ~ WHERE ~ toEqualsSQL(fields.head)
  }
  
  def TableRow(name: Symbol, fields: (Symbol, SQL)*): TableRow = TableRow(name, fields.toList)
  
  def DML(text: String): SQL = new SQL(text) {
    override def apply(items: SQL*) = SQL( items.map(_.text).mkString(text  + " ", ", ", ""))
  }
  
  def ID(names: String*):SQL = SQL(names.map(_.toUpperCase).mkString("."))
  def ID(first: Symbol, second: Symbol):SQL = ID(first.name, second.name)
  implicit def ID(value: Symbol): SQL = ID(value.name)

  def toEqualsSQL(p: (Symbol, SQL)) = SQL(ID(p._1.name).text + " = " + p._2.text)
  def toColumnSQL(p: (Symbol, SQL)) = SQL(ID(p._1.name).text + " " + p._2.text)
  
  implicit def toString( s: SQL) = s.text
  implicit def toSQL(value: String): SQL = quote(value)
  implicit def toSQL(value: Boolean): SQL = if(value) SQL("'Y'") else SQL("'N'")
  implicit def toSQL(value: Int): SQL = SQL(value.toString)
  implicit def toSQL(value: Long): SQL = SQL(value.toString)
  implicit def toSQL(value: Double): SQL = SQL(value.toString)
  implicit def toSQL(value: Date): SQL = toSQL(new Timestamp(value.getTime))
  implicit def toSQL(value: Timestamp): SQL = TO_TIMESTAMP(value.toString, "YYYY-MM-DD HH24:MI:SS.FF")
  implicit def toSQL(value: (Symbol, SQL)): SQL = toColumnSQL(value)
  implicit def toSQL( t: Table): SQL = t.toCreateSQL
  implicit def toSQL( r: TableRow ): SQL = r.toInsertSQL
  implicit def toSQL(value: Option[String]): SQL = value match {
    case Some(v) => toSQL(v)
    case None => NULL
  }
  implicit def toSQLFromDate(value: Option[Date]): SQL = value match {
    case Some(v) => toSQL(v)
    case None => NULL
  }
  
  implicit def toSQL(p: Point)(implicit coords: Coords): SQL = {
    p match { case coords(x, y) => SDO_GEOMETRY(2001, coords.srid, SDO_POINT_TYPE(x,y, NULL), NULL, NULL) }
  }
  
  implicit def toSQL(shape: Shape)(implicit coords: Coords): SQL = { 

    Spatial.shapeToOrds(shape) match {
      case Some((gtype, info, ords)) =>
        SDO_GEOMETRY(gtype, if(coords.srid == 0) NULL else coords.srid, NULL, SDO_ELEM_INFO_ARRAY(info.map(toSQL(_)):_*), SDO_ORDINATE_ARRAY(ords.map(toSQL(_)):_*))
      case None => 
        NULL
    }
  }
  
  val SRID_WGS_84 = 8307
  val SRID_MGA_ZONE_56 = 28356
  
  val CREATE = SQL("CREATE")
  val ALTER = SQL("ALTER")
  val DROP = SQL("DROP")
  val GRANT = SQL("GRANT")

  val TABLE = SQL("TABLE")
  val VIEW = SQL("VIEW")
  val USER = SQL("USER")
  val SESSION = SQL("SESSION")
  val INDEX = SQL("INDEX")
  val SEQUENCE = SQL("SEQUENCE")

  val AS = SQL("AS")
  val ALL = SQL("ALL")
  val ON = SQL("ON")
  val IS = SQL("IS")
  val IN = SQL("IN")
  val BY = DML("BY")
  val TO = SQL("TO")
  val ANY = SQL("ANY")
  val AND = SQL("AND")  
  val OR = SQL("OR")
  val LIKE = SQL("LIKE")
  val NOT = SQL("NOT")
  val EQ = SQL( "=" )
  val P = SQL("?")
  val LEFT = SQL("LEFT")
  val OUTER = SQL("OUTER")
  val INNER = SQL("INNER")
  val JOIN = SQL("JOIN")
  val FROM = DML("FROM")
  val WHERE = DML("WHERE")
  val ORDER = SQL("ORDER")
  val INTO = SQL("INTO")
  val SET = DML("SET")
  val ADD = SQL("ADD")
  val VALUES = SQL("VALUES")
  val BEGIN = SQL("BEGIN")
  val END = SQL("END")
  val IDENTIFIED = SQL("IDENTIFIED")
  val DEFAULT = SQL("DEFAULT")
  val TABLESPACE = SQL("TABLESPACE")
  val QUOTA = SQL("QUOTA")
  val UNLIMITED = SQL("UNLIMITED")
  val EXECUTE = SQL("EXECUTE")

  val SELECT = DML("SELECT")
  val DISTINCT = DML("DISTINCT")
  val INSERT = SQL("INSERT")
  val DELETE = SQL("DELETE")
  val UPDATE = SQL("UPDATE")
  
  val VARCHAR2 = SQL("VARCHAR2")
  val NUMBER = SQL("NUMBER")
  val FLOAT = SQL("FLOAT")
  val TIMESTAMP = SQL("TIMESTAMP")
  val TO_TIMESTAMP = SQL("TO_TIMESTAMP")
  
  val NULL = SQL("NULL")
  val TRUE = SQL("TRUE")
  val FALSE = SQL("FALSE")

  val PRIMARY = SQL("PRIMARY")
  val KEY = SQL("KEY")
  val INDEXTYPE = SQL("INDEXTYPE")
  val CASCADE = SQL("CASCADE")

  val SDO_DIM_ARRAY = ID("SDO_DIM_ARRAY")
  val SDO_DIM_ELEMENT = ID("SDO_DIM_ELEMENT")
  val CREATE_LOGICAL_NETWORK = ID("SDO_NET", "CREATE_LOGICAL_NETWORK")
  val SDO_ELEM_INFO_ARRAY = ID("SDO_ELEM_INFO_ARRAY")
  val SDO_ORDINATE_ARRAY = ID("SDO_ORDINATE_ARRAY")
  val SDO_POINT_TYPE = ID("SDO_POINT_TYPE")
  val SDO_GEOMETRY = ID("SDO_GEOMETRY")
  val SDO_FILTER = ID("SDO_FILTER")
}
