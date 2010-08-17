package au.com.langdale
package eclipse

import org.eclipse.jface.wizard.{Wizard, IWizardPage}
import org.eclipse.ui.IWorkbenchWizard
import org.eclipse.ui.IWorkbench
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.operation.IRunnableContext
import scala.collection.mutable.ListBuffer

abstract class WizardBuilder extends Wizard with IWorkbenchWizard with Disposable {
  
  def init( w: IWorkbench, s: IStructuredSelection) {}
  def shell = getContainer.getShell
  def context: IRunnableContext = getContainer
  def doRefresh = pages foreach { case f: FormBuilder => f.assembly.doRefresh case _ => }
  
  setNeedsProgressMonitor(true)
  
  val pages = new ListBuffer[IWizardPage]
  
  override def addPages {
    for( page <- pages ) addPage(page)
  }
  
  override def dispose {
	super.dispose
	disposeChain
  }
}
