package au.com.langdale
package eclipse

import org.eclipse.ui.services.IDisposable
import java.sql.Connection
import db.ConnectionHolder._
import db.DBA._
import scala.actors.Actor
import actors.Operation._
import db.SQLQuoter._

trait DatabasePart[T <: AnyRef] extends EventMonitor with Subscriber {
  case class QueryStatus(message: String)
  case class QueryResult( rows: List[T], selection: List[T])
  case class QueryIssue( workspace: String, savepoint: Option[String], query: Query[T], criterion: T => Boolean)
  def QueryIssue( workspace: String, query: Query[T], criterion: T => Boolean): QueryIssue = QueryIssue(workspace, None, query, criterion)

  val key: String
  
  private var pending: Option[QueryIssue] = None
  
  def startQuery {
    for( QueryIssue(workspace, _, _, _) <- pending) { 
      localcast ! QueryStatus("Pending...")
      broadcast ! Take(key, workspace)
    }
  }
  
  val db = subscribe {

    case issue: QueryIssue => 
      pending = Some(issue)
      startQuery

    case ConnectionChange(`key`) => startQuery
      
    case event: Notify => localcast ! event
    
    case GeneralNotify => localcast ! GeneralNotify
      
    case ConnectionProblem(`key`, descr) =>  
      localcast ! QueryStatus("Connection error..." + descr)
      
    case Give(`key`, workspace, connection) => 
      for( QueryIssue(`workspace`, savepoint, query, criterion) <- pending) localcast ! {
        try {
          for( s <- savepoint) { connection call ID('DBMS_WM, 'GotoSavepoint)(s) }
          val result = connection.query(query).toList
          QueryResult(result, result filter criterion)
        }
        catch {
          case ex: Exception => QueryStatus(ex.getMessage)
        }
      }
      broadcast ! Release(key, connection)
  }
}
