package au.com.langdale
package eclipse

import org.eclipse.swt.widgets.Composite
import org.eclipse.jface.viewers.{ISelection, IStructuredSelection, IDoubleClickListener, DoubleClickEvent}
import org.eclipse.ui.{IWorkbenchPart, ISelectionListener}
import org.eclipse.swt.events.{SelectionListener, SelectionEvent}
import org.eclipse.ui.forms.events.{IHyperlinkListener, HyperlinkEvent}
import au.com.langdale.ui.plumbing.{Binding,Observer}
import au.com.langdale.ui.builder.ButtonObserver

object EventMonitor {
  case class SingleSelection(value: AnyRef)
  case class Click(target: String)
  case class DoubleClick(target: AnyRef)
  case class Enter(target: String)
  case class Exit(target: String)
  case object Refresh
  case object Reset
  case object Update
  case object Valid
  case object Dirty
  case class Invalid(message: String) extends Exception(message)
}
import EventMonitor._

trait EventMonitor extends UIActor {
  
  val localcast: UIActor = this
  
  def monitorButton(event: String) = new SelectionListener {
    def widgetDefaultSelected( e: SelectionEvent) { localcast ! Click(event) }
    def widgetSelected( e: SelectionEvent) { localcast ! Click(event) }
  }
  
  object monitorLinks extends IHyperlinkListener {
    def linkEntered( e: HyperlinkEvent ) { localcast ! Enter(e.getHref.toString) }
    def linkExited( e: HyperlinkEvent ) { localcast ! Exit(e.getHref.toString) }
    def linkActivated( e: HyperlinkEvent ) { localcast ! Click(e.getHref.toString) }
  }
  
  object monitorClicks extends ButtonObserver {
     def entered( name: String ) { localcast ! Enter(name) }
     def exited( name: String ) { localcast ! Exit(name) }
     def clicked( name: String ) { localcast ! Click(name) }
  }
  
  object monitorPlumbing extends Binding with Observer {
    def validate: String = null
    def refresh { localcast ! Refresh }
    def reset { localcast ! Reset }
    def update { localcast ! Update }
    def markInvalid( message: String ) { localcast ! Invalid(message) }
    def markValid { localcast ! Valid }
    def markDirty { localcast ! Dirty }
  }
  
  object monitorDoubleClicks extends IDoubleClickListener {
    def doubleClick(e: DoubleClickEvent) = e.getSelection match { 
      case sel: IStructuredSelection if sel.size == 1 => localcast ! DoubleClick( sel.getFirstElement ) 
      case _ =>
    }
  }
  
  object monitorSelection extends ISelectionListener {
    def selectionChanged( part: IWorkbenchPart, sel: ISelection) {
      sel match {
        case sel: IStructuredSelection if sel.size == 1 =>
          localcast ! SingleSelection(sel.getFirstElement)
          
        case _ =>
      }
    }
  }
}
