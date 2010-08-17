package au.com.langdale
package eclipse
import org.eclipse.ui.{IWorkbenchWindowActionDelegate,IWorkbenchWindow}
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.operation.IRunnableContext

trait ActionDelegate extends IWorkbenchWindowActionDelegate {
  
  def shell = w.getShell
  def context: IRunnableContext = w
  def page = w.getActivePage
  def window = w
  private var w: IWorkbenchWindow = _
  
  def init(w: IWorkbenchWindow) {this.w = w}
  def selectionChanged(action: IAction, selection: ISelection) {}
  def dispose {}
}
