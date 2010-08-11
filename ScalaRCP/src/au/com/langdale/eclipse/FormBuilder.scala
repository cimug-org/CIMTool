package au.com.langdale
package eclipse
import ui.builder.{Assembly}
import scala.xml.{Node, NodeSeq}
import org.eclipse.swt.widgets.{Composite, Control}
import util.{Descriptor}
import EventMonitor._
import ui.plumbing.Binding

trait FormBuilder extends TemplateBuilder with EventMonitor {
  
  def toolkit = Assembly.createFormToolkit
  
  val assembly = new Assembly( toolkit, monitorPlumbing, true)
  val template: Node
  
  def createPartControl( parent: Composite ) {
    assembly.addBinding(monitorPlumbing)
    assembly.setButtonObserver(monitorClicks)
    assembly.realise(parent, buildTemplate(template))
  }
  
  def validate( rule: => Option[String]) {
    assembly addBinding new Binding {
      def validate: String = {
        rule match {
          case Some(message) => message
          case None => null
        }
      }
      def refresh {}
      def reset {}
      def update {}
    }
  }
  
  def refresh( action: => Unit) {
    assembly addBinding new Binding {
      def validate: String = null
      def refresh { action }
      def reset {}
      def update {}
    }
  }
  
  def update( action: => Unit) {
    assembly addBinding new Binding {
      def validate: String = null
      def refresh {}
      def reset {}
      def update { action }
    }
  }
  
  def reset( action: => Unit) {
    assembly addBinding new Binding {
      def validate: String = null
      def refresh {}
      def reset { action }
      def update {}
    }
  }
  
  def validate( message: String)(check: => Boolean) { validate { if(check) None else Some(message) }}
  
  def detail[T](desc: Descriptor[T], value: => T): NodeSeq = detail(desc, value, "", true)
  
  def detail[T](desc: Descriptor[T], value: => T, prefix: String, enable: => Boolean): NodeSeq = {
    for(desc.Prop(title, name, get) <- desc.toList.reverse) {
      reset { assembly.setTextValue(prefix + name, "") }
      refresh { assembly.setTextValue(prefix + name, get(value).toString) }
      validate(title + " must be given") { ! enable || ! get(value).toString.isEmpty } 
    }

    for(desc.Prop(title, name, _) <- desc.toList) yield 
      <group>
         <label>{title}</label>
         <field href={prefix + name} />
      </group>
  }
  
  def setFocus {assembly.getControl("focus") match { case null => case c => c.setFocus }}

}
