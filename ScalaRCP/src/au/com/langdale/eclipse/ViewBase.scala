package au.com.langdale
package eclipse
import org.eclipse.ui.IViewPart
import org.eclipse.jface.operation.IRunnableContext
import org.eclipse.swt.widgets.Composite
import org.eclipse.jface.action.Action
import ui.util.IconCache
import scala.collection.mutable.ListBuffer

trait ViewBase extends IViewPart {
  
  def shell = getSite.getShell
  def context: IRunnableContext = getSite.getWorkbenchWindow
  
  abstract override def createPartControl( parent: Composite ) {
    super.createPartControl(parent)
    val toolbar = getViewSite.getActionBars.getToolBarManager
    tools foreach { toolbar add _ }
  }
  
  val tools = new ListBuffer[Action]
  
  def action(name: String, text: String)( imp: =>Unit ) = {
    val a = new Action { override def run = imp }
    a setId name
    a setImageDescriptor IconCache.getIcons.getDescriptor(name, false, 16)
    a setText text
    tools += a
    a
  }

}
