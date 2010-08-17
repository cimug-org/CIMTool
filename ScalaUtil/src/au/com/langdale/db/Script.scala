package au.com.langdale
package db
import java.io.{BufferedReader, InputStreamReader, InputStream}
import DBA._

object Script {
  def run(spec: Spec, stream: InputStream) {
    val db = spec.connect
    db.setAutoCommit(false)
    
    try {
    
      val r = new BufferedReader( new InputStreamReader(stream))
      try {
        var line = r.readLine
        while(line != null) {
          val sql = line.trim
          if( sql != "")
            db exec SQL(sql)
          line = r.readLine
        }
      }
      finally {
        r.close
      }
      
      db.commit
    }
    finally {
      db.rollback
      db.close
    }
  }
}
