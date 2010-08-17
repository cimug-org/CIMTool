package au.com.langdale
package collection

class FilteredIterator[A](val inner:Iterator[A])(val p: A => Boolean) extends Iterator[A] {
  private var filled = false
  private var exhausted = false
  private var lookahead: A = _
  
  def hasNext: Boolean = {
    if( ! filled && ! exhausted) {
      while( inner.hasNext ) {
        lookahead = inner.next
        if(p(lookahead))
          return true
      }
      exhausted = true
    }
    ! exhausted 
  }

  def next: A = {
    hasNext
    if(exhausted)
      throw new NoSuchElementException
    filled = false
    lookahead
  }
}  
