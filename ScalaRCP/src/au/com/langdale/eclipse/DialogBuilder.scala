package au.com.langdale
package eclipse
import org.eclipse.swt.widgets.Composite
import org.eclipse.jface.wizard.WizardPage
import EventMonitor._
import ui.builder.{Assembly}

abstract class DialogBuilder(name: String) extends WizardPage(name) with Disposable with FormBuilder   {
  
  override def toolkit = Assembly.createDialogToolkit
    
  setDescription(name)
  
  def createControl( parent: Composite ) {
    createPartControl(parent)
    setControl(assembly.getRoot)
    assembly.doRefresh
  }
  
  def shell = getContainer.getShell
  def context = getContainer
  
  localcast listen {
    case Valid => 
      setErrorMessage(null)
      setPageComplete(true)
      getNextPage match {
        case page: FormBuilder => page.assembly.fireValidate
        case _ =>
      }
      
    case Invalid(message) => 
      setErrorMessage(if(message == "") null else message)
      setPageComplete(false)
  }
  
  override def dispose {
	super.dispose
	disposeChain
  }
}
