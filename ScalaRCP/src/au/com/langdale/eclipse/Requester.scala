package au.com.langdale
package eclipse
import actors.{Client, Operation}

trait Requester extends Progress with Client {
 
  def request(op: Operation, title: String): Boolean = 
    alert(title) { block(title) { request(op) }}
  
  def requestConfirm(op: Operation, title: String): Boolean = 
    alert(title) { confirm(title) { block(title) { request(op) }}}
}
