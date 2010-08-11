package au.com.langdale.util
import scala.collection.mutable.{HashMap, Map, Set}

class Grouper[Node] {
  val islands:Map[Node, Int] = new HashMap
  val nodes:Map[Int, Set[Node]] = new HashMap
  
  private var retired = 0
  private var created = 0
  
  private def assign(n:Node, i:Int) {
    islands += Pair(n, i)
    nodes(i) += n
  }
  
  private def merge(i:Int, j:Int) {
    if(i == j) return
      
    val ns = nodes(i)
    val ms = nodes(j)
    
    if(ns.size > ms.size) {
      merge(j, i)
      return
    }
    
    for( n <- ns) islands += Pair(n, j)
    ms ++= ns
    nodes -= i
    retired += 1
  }
  
  private def create(s:Node, o:Node) {
    nodes += Pair(created, Set(s, o))
    islands += Pair(s, created)
    islands += Pair(o, created)
    created += 1
  }
  
  def size = created - retired

  def add(s:Node, o:Node) {
    if( islands.contains(s)) {
      if( islands.contains(o)) 
        merge(islands(s), islands(o))
      else
        assign(o, islands(s))
    }
    else {
      if( islands.contains(o)) 
        assign(s, islands(o))
      else
        create(s, o)
    }
  }
  
  def addAll( sos: Iterable[(Node, Node)]) {
    for((s, o) <- sos) add(s, o)
  }
  
  def clear = {
    islands.clear
    nodes.clear
    created = 0
    retired = 0
  }
}
