package au.com.langdale
package collection

import scala.collection.mutable.{HashMap,HashSet,Set}

class SymetricProperty[Node] extends HashMap[Node,Set[Node]] {
  def add(s:Node, o:Node) {
    getOrElseUpdate(s, new HashSet[Node]) += o
    getOrElseUpdate(o, new HashSet[Node]) += s
  }
}
