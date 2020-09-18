package au.com.langdale
package eclipse
import ui.builder.{Template}
import ui.builder.Templates._
import scala.xml.{Node, Elem, Text}

trait TemplateBuilder {
  
  def buildTemplate(node: Node) = prepare(node).get
  
  private def href(node: Node) = attr("href", node, null)
  private def image(node: Node) = attr("image", node, null)
  private def title(node: Node) = attr("title", node, null)
  private def lines(node: Node) = attr("lines", node, "0").toInt
  private def select(node: Node) = attr("select", node, "") == "multiple"
                                       
  private def attr(key: String, node: Node, default: String): String = node.attributes(key) match {
    case null => default
    case Text(s) => s
  }

  private def children( ns: Seq[Node]) = ns flatMap {n => prepare(n)} toArray
  private def child( ns: Seq[Node]) = ns flatMap { n => prepare(n)} first
  
  private def prepare(node: Node): Option[Template] = node match {
    case <grid>{ ns @  _* }</grid>      => Some(Grid(ns.flatMap (n => group(n)).toArray :_*))
    case <stack>{ ns @  _* }</stack>    => Some(Stack(children(ns)))
    case <row>{ ns @  _* }</row>        => Some(Row(children(ns)))
    case <focus>{ ns @ _* }</focus>     => Some(Mark("focus", child(ns)))                                     
    case <right>{ ns @ _* }</right>     => Some(Right(child(ns)))                                     
    case <vbar/>                        => Some(VBar())
    case <hrule/>                       => Some(HRule())
    case n @ <form>{ ns @ _* }</form>   => Some(Form(image(n), title(n), child(ns)))
    case n @ <display>{ ns @ _* }</display> => Some(Markup( href(n), <form>{ns}</form>.toString, lines(n))) 
    case n @ <textarea/>                => Some(TextArea(href(n), lines(n), true))
    case n @ <displayarea/>             => Some(DisplayArea(href(n), lines(n)))
    case n @ <checkboxtable/>           => Some(CheckboxTableViewer(href(n), select(n)))
    case n @ <checkboxtree/>            => Some(CheckboxTreeViewer(href(n), select(n)))
    case n @ <tree/>                    => Some(TreeViewer(href(n), select(n)))
    case n @ <table/>                   => Some(TableViewer(href(n)))
    case n @ <image/>                   => Some(Image(href(n), image(n)))
    case n @ <textfield/>               => Some(DisplayField(href(n)))
    case n @ <field>{ ns @ _* }</field> => Some(Field(href(n), ns.text))
    case n @ <label>{ ns @ _* }</label> => Some(Label(href(n), ns.text))
    case n @ <pushbutton>{ ns @ _* }</pushbutton>   => Some(PushButton(href(n), ns.text, image(n)))
    case n @ <radiobutton>{ ns @ _* }</radiobutton> => Some(RadioButton(href(n), ns.text))
    case n @ <checkbox>{ ns @ _* }</checkbox>       => Some(CheckBox(href(n), ns.text, image(n)))
    case n: Elem =>
      Log("Unrecognised term in form template: " + n.label)
      None
    case _ => None
  }
  
  private def group(node: Node): Option[GroupTemplate] = node match {
    case <group>{ ns @  _* }</group> => Some(Group(children(ns)))
    case n => prepare(n) map { t => Group(t) }
  }

}
