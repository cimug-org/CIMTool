package au.com.langdale
package eclipse
import org.eclipse.jface.operation.{IRunnableContext,IRunnableWithProgress}
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jface.dialogs.MessageDialog.{openError, openConfirm, openInformation}
import org.eclipse.swt.widgets.Shell

trait Progress {
  def shell: Shell
  def context: IRunnableContext

  def block[A](title: String)( action: => Option[A] ): Option[A] = {
    var result: Option[A] = None
    
    context.run( true, false, new IRunnableWithProgress {
       def run( monitor: IProgressMonitor) { 
         monitor.beginTask(title, IProgressMonitor.UNKNOWN)
         result = action 
         monitor.done
       }       
    })
    
    result
  }
  
  def confirm[A](title: String)( action: => Option[A] ): Option[A] = {
    if( openConfirm(shell, title, title + ". Confirm?")) 
      action
    else
      None
  }
  
  def alert(title: String)( mesg: Option[String]): Boolean = {
    mesg match {
      
      case Some(text) => 
        openError(shell, title, text)
        false
        
      case None => true
    }
  }
  
  def notice(title: String)( mesg: Option[String]): Boolean = {
    mesg match {
      
      case Some(text) => 
        openError(shell, title, text)
        false
        
      case None => 
        openInformation(shell, title, "Completed OK.")
        true
    }
  }
}
