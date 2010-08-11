package au.com.langdale
package eclipse
import org.eclipse.ui.PlatformUI
import java.lang.Thread.currentThread

object UIActor {
  
  def asyncExec( action: =>Unit) {
    val display = PlatformUI.getWorkbench.getDisplay

    if( currentThread == display.getThread  )
      action
    else
      display asyncExec new Runnable { def run = action }
  }
}
import UIActor._

trait UIActor extends DisposeChain {
  type Reaction = PartialFunction[Any,Unit]
  private var reactions: List[Reaction] = Nil

  def !( mesg: Any) = asyncExec { reactions filter(_.isDefinedAt(mesg)) foreach ( _.apply(mesg)) }
  
  def listen(r: Reaction) = asyncExec { reactions = r :: reactions }
  
  abstract override def disposeChain {
	super.disposeChain
    asyncExec { reactions = Nil }
  }
}
