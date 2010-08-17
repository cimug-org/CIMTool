package au.com.langdale
package eclipse

import org.eclipse.ui.{IWorkbenchPart}
import org.eclipse.swt.widgets.{Composite}

trait SelectionMonitor extends EventMonitor with IWorkbenchPart {
  
  abstract override def createPartControl(parent: Composite) {
    super.createPartControl(parent)
    getSite.getPage.addSelectionListener(monitorSelection)
  }

  abstract override def dispose {
    getSite.getPage.removeSelectionListener(monitorSelection)
    super.dispose
  }
}
