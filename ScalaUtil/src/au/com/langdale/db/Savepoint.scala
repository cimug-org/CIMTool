package au.com.langdale
package db

import java.util.Date
import java.sql.{Connection,ResultSet}
import db.SQLQuoter._
import db.DBA._

case class Savepoint(name: String, workspace: String, date: Date, desc: String, avail: String) {
  def rollback = ID("DBMS_WM", "RollbackToSP")(workspace, name)
  def create = ID('DBMS_WM, 'CreateSavepoint)( workspace, name, desc )
  def delete = ID('DBMS_WM, 'DeleteSavepoint)( workspace, name )
  def difference(ref: Savepoint) = ID('DBMS_WM, 'SetDiffVersions)( ref.workspace, ref.name, workspace, name)
  def diffLatest = ID('DBMS_WM, 'SetDiffVersions)( workspace, name, workspace, "LATEST")
  lazy val identifier = workspace + ", " + name
  lazy val latestIdentifier = workspace + ", LATEST"
}

object Savepoint extends RecordType[Savepoint] {
  
  val name = Prop("Name", "savepoint", _.name)
  val workspace = Prop("Workspace", "workspace", _.workspace)
  val date = Prop("Created", "createTime", _.date)
  val desc = Prop("Description", "description", _.desc)
  val avail = Prop("Available", "canRollbackTo", _.avail)
  
  def apply(implicit rs: ResultSet): Savepoint = apply(name, workspace, date, desc, avail)
  
  def byWorkspace( ws: String ) = this query SELECT(name, workspace, date, desc, avail) ~ 
    				FROM('ALL_WORKSPACE_SAVEPOINTS) ~
                    WHERE ~ 'WORKSPACE ~ EQ ~ ws ~
                    ORDER ~ BY ~ 'POSITION 
  
  val live = byWorkspace(LIVE) 
  
}
