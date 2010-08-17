package au.com.langdale
package db

import java.util.Date
import java.sql.{Connection,ResultSet}
import db.SQLQuoter._
import db.DBA._

case class Workspace(name: String, date: Date, desc: String) {
  def create = ID("DBMS_WM", "CreateWorkspace")(name, desc)
  def update = ID("DBMS_WM", "AlterWorkspace")(name, desc)
  def delete = ID("DBMS_WM", "RemoveWorkspace")(name)
  def merge = ID("DBMS_WM", "MergeWorkspace")(name, TRUE)
  
}

object Workspace extends RecordType[Workspace] {
  
  val name = Prop("Name", "workspace", _.name)
  val date = Prop("Created", "createTime", _.date)
  val desc = Prop("Description", "description", _.desc)
  
  def apply(implicit rs: ResultSet): Workspace = apply(name, date, desc)
  
  val all = this query SELECT(name, date, desc) ~ FROM('ALL_WORKSPACES) ~
                       WHERE ~ name ~ SQL("<>") ~ LIVE ~
                       ORDER ~ BY ~ name 
  
  //def byName(crit: String) = this query all.sql ~ WHERE ~ name ~ LIKE ~ crit
}
