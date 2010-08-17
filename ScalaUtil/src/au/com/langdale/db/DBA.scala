package au.com.langdale
package db

import java.sql.{Connection, SQLException, ResultSet, PreparedStatement}
 
object DBA {
  implicit def connection2Rich(c: Connection) = new RichConnection(c)

  case class SQL(text: String) { 
    def ~(other: SQL) = SQL( text + " " + other.text)
    def apply(items: SQL*) = SQL( items.map(_.text).mkString(text + "(", ", ", ")"))
  }
  
  def quote(value: String): SQL = SQL("'" + value.replace("'", "''") + "'")  

  def bracket(func: SQL) = SQL("BEGIN") ~ func ~ SQL(";") ~ SQL("END") ~ SQL(";") 

  def logExcept[A](mesg: String)( op: => A ) = {
    try {
      op	
    }
    catch {
      case e: SQLException => 
        Log.error("Error executing: " + mesg, e)
        throw e
    }
  }
  
  final val LIVE = "LIVE"
  
  trait Query[+C] {
    val sql: SQL
    def fetch( implicit results: ResultSet ): C
  }
  
  class StatementTemplate( db: Connection, sql: SQL ) {
    val statement = db prepareStatement sql.text
    
    def apply( params: Any* ) {
      for( (p, i) <- params.toList.zipWithIndex )
        statement.setObject(i+1, p)
      
      val t = sql.text + ": ? <- " + params.mkString(", ")
      println(t)
      logExcept(t) {
        statement.execute
      }
    }
    
    def close = statement.close
  }
  
  
  class RichConnection( inner: Connection) {
    
    def query[C]( q: Query[C] ): Iterator[C] = {
      new ResultIterator(inner, q)  
    }
    
    def exec( s: SQL ) {
      println(s.text)
      logExcept(s.text) { execStat(s) }
    }
    
    def execStat( s: SQL ) {
      val st = inner.createStatement
      try {
        st.executeUpdate(s.text.trim)
      }
      finally {
        st.close
      }
    }
    
    def prepare( s: SQL ) = logExcept(s.text) {new StatementTemplate( inner, s ) }
    
    def goto( ws: String ) = logExcept("Goto workspace " + ws) { execStat(bracket(SQL("DBMS_WM.GotoWorkspace")(quote(ws)))) }
    
    def call(func: SQL) { exec(bracket(func)) }
    
    def maybe( s: SQL ) = {
      try { 
        execStat(s); true 
      } catch { 
        case e: SQLException => false
      }
    }

    def callMaybe(func: SQL) { maybe(bracket(func)) }
  }
  
  def withConnection[A](spec: Spec)( action: Connection => A): A = {
    val db = spec.connect
    db setAutoCommit false
    try {
      val a = action(db)
      db.commit
      a  
    }
    finally {
      db.rollback
      db.close
    }
  } 

  private[DBA] class ResultIterator[A](c: Connection, q: Query[A]) extends Iterator[A] {

    private var filled = false
    private var valid = true

    lazy val st = c.createStatement
    
    lazy val inner = {
      val t = q.sql.text.trim
      
      try {
        println(t)
        st.executeQuery(t)
      }
      catch {
        case e: SQLException =>
          Log.error("Error executing: " + t, e)
          st.close
          throw e 
      }
    }
 
    def advance {
      if( valid && ! filled ) {
        valid = inner.next
        if(! valid )
          inner.close
      }  
    }
    
    def hasNext = {
      advance
      filled = true
      valid
    }
  
    def next = {
      advance
      filled = false
      if( valid) {
        try {
          q fetch inner
        }
        catch {
          case e: Exception => 
            Log.error("Error converting query results" , e)
            st.close
            valid = false
            throw e
        }
      }  
      else
        throw new NoSuchElementException
    }
  }
}
