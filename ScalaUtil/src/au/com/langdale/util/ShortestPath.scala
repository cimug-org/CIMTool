package au.com.langdale
package util
import Graph._
import scala.collection.mutable.{HashMap, HashSet, PriorityQueue}
import scala.collection.Map
import Math._

object ShortestPath {
  
  def search(g: Graph, starts: Set[Node], boundary: Set[Node], distance: Edge => Double, solutionLimit: Int, costLimit: Double) = {
    
    var solutionCount = 0
    
    var maxCost = costLimit
    
    val cost = new HashMap[Node, Double] {
      override def default(n: Node) = MAX_DOUBLE
    }
    
    val paths = new HashMap[Node, Edge]
    
    implicit val ordering = new Ordering[(Node, Double)] { 
      def compare(nc1: (Node, Double), nc2: (Node, Double)) = - (nc1._2 - nc2._2).toInt  
    }
    
    val queue = new PriorityQueue()
    
    val closed = new HashSet[Node]
    
    def update(nc: (Node, Double)) {
      cost += nc
      queue += nc
      
      if( boundary contains nc._1) {
        maxCost = min(costLimit, boundary.foldLeft(0.0) {(c, n) => max(c, cost(n))})
      }
    }
    
    for( n <- starts) update( n -> 0.0 )
    
    while(! queue.isEmpty && solutionCount < solutionLimit) {
      val (n1, c1) = queue.dequeue

      if(! (closed contains n1)) {
        closed += n1
        
        if( boundary contains n1) {
          solutionCount += 1
        }
        else if( c1 < maxCost ) {
          for(e <- g.edges(n1); n2 = opposite(n1, e); if ! (closed contains n2)) {
            val c2 = c1 + distance(e)
            if( cost(n2) > c2) {
              update(n2 -> c2)
              paths(n2) = e
            }
          }
        }
      }
    }
    
    (cost, paths)
  }
}
