package au.com.langdale
package eclipse
import org.eclipse.ui.part.ViewPart
import org.eclipse.jface.operation.IRunnableContext
import org.eclipse.swt.widgets.Composite
import org.eclipse.jface.action.Action
import scala.collection.mutable.ListBuffer

abstract class ViewBuilder extends ViewPart with Disposable with FormBuilder with ViewBase {
  override def createPartControl( parent: Composite ) {
    super.createPartControl(parent)
    assembly.doRefresh
  }
  
  override def dispose {
	super.dispose
	disposeChain
  }
}
