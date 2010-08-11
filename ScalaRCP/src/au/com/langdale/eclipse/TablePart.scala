package au.com.langdale
package eclipse

import org.eclipse.swt.widgets.Composite
import org.eclipse.jface.viewers.{StructuredViewer, IStructuredSelection, StructuredSelection}
import org.eclipse.ui.IWorkbenchPart

import ui.builder.Assembly
import ui.builder.Template
import ui.builder.Templates.{TableViewer, Stack, DisplayArea}
import util.Descriptor
import db.DBA.Query

trait TablePart[T <: AnyRef] extends FormBuilder with DatabasePart[T] {
  
  val descr: Descriptor[T]
  val checkboxes: Boolean
  
  private lazy val binding = TableBinding(descr, checkboxes) 
  
  lazy val tableTemplate = 
    <stack>
  	  { if(checkboxes) 
          <checkboxtable href="view" select="multiple"/> 
        else 
          <table href="view"/> }
      <display href="mesg">Nothing selected</display>
    </stack>
    
  def augmentQueryResult(value: List[T]) = value  
     
  localcast listen {
    case QueryResult(value, selection) => 
      binding setInput augmentQueryResult(value)
      selectionProvider.getControl.setEnabled(true)
      assembly showStackLayer "view" 
      assembly.getMarkup("mesg").setText("", false, false)
      assembly.doRefresh
      binding.setValues(selection.toArray)  
        
    case QueryStatus(value) => 
      selectionProvider.getControl.setEnabled(false)
      assembly.getMarkup("mesg").setText(value, false, false)
      assembly showStackLayer "mesg"
      assembly doRefresh
  }
  
  def selectionProvider = assembly.getViewer("view")
  
  def selection = binding.selection
  def contents = binding.contents
  
  override def createPartControl(parent: Composite) { 
    super.createPartControl(parent)
    binding.bind("view", assembly)
    assembly.getViewer("view").asInstanceOf[StructuredViewer].addDoubleClickListener(monitorDoubleClicks)
  }
}
