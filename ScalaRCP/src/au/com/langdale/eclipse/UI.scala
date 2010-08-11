package au.com.langdale
package eclipse
import org.eclipse.swt.widgets.Composite

trait UI {
  //def createPartControl(parent: Composite): Unit
  def dispose: Unit
}

//object UI {
//  trait Part extends UI {
//    def createControl(parent: Composite) = createPartControl(parent)
//  }  
//}