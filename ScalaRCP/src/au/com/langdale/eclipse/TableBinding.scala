package au.com.langdale
package eclipse
import ui.binding.{AnyModel, ArrayModel, ListBinding, TableBinding => ChecksBinding}
import ui.plumbing.{Binding}
import ui.builder.{Assembly}
import ui.util.IconCache
import util.Descriptor

import org.eclipse.jface.viewers.{StructuredViewer,Viewer,TableViewer,TableViewerColumn,LabelProvider,ITableLabelProvider,IStructuredContentProvider}
import org.eclipse.swt.SWT

object TableBinding {
  def apply[T <: AnyRef](cols: Descriptor[T], checkboxes: Boolean) =
    if( checkboxes )
      new ChecksBinding with TableBinding[T] { val descr = cols }
    else
      new ListBinding with TableBinding[T] { 
        val descr = cols
        var plumbing: Assembly = _
        
        def getValues: Array[AnyRef] = 
          if( getValue == null ) 
            Array()
          else
            Array(getValue)
        
        def setValues(values: Array[AnyRef]) {
          if( values.isEmpty )
            setValue(null)
          else
            setValue(values(0))
          plumbing.doRefresh
        }
        
        override def bind( name: String, assembly: Assembly) {
          plumbing = assembly
          super.bind(name, assembly)
        }
      }
  
  
}


trait TableBinding[T <: AnyRef] extends Binding with AnyModel with ArrayModel  {
  val descr: Descriptor[T]
  
  private var input: Seq[AnyRef] = Nil
  private lazy val props = descr.toList.toArray
  
  def bind( name: String, assembly: Assembly)
  def getInput = input
  def setInput( given: Seq[AnyRef]) { input = given }

  def configureViewer(raw: StructuredViewer) {
     raw match {
       case viewer: TableViewer =>
         for( prop <- props ) {
           val vcol = new TableViewerColumn(viewer, SWT.NONE)
           val tcol = vcol.getColumn
           tcol setText prop.desc
           tcol setResizable true
           tcol setMoveable true
           tcol setWidth 100
         }
         val table = viewer.getTable
         table setHeaderVisible true
         table setLinesVisible true
         viewer setLabelProvider labels
         viewer setContentProvider provider
     }
  }
  
  private def truncate(text: String) = {
    val ix = text.indexOf("\n")
    val iy = if( ix == -1 ) if(text.length > 50) 50 else text.length else ix
    text.substring(0, iy)
  }
  
  private object labels extends LabelProvider with ITableLabelProvider {
    def getColumnText( raw: Any, ix: Int) = props(ix).get(raw.asInstanceOf[T]) match {
      case null => ""
      case Some(value) => truncate(value.toString.trim)
      case None => ""
      case value => truncate(value.toString.trim)
    } 
    def getColumnImage( raw: Any, ix: Int) = if( ix == 0 ) IconCache.getIcons.get(raw) else null
  }
  
  private object provider extends IStructuredContentProvider {
    def getElements(i: Any) = input.toArray[AnyRef]
    def inputChanged(v: Viewer, o: Any, n: Any) {}
    def dispose {}
  }
  
  def selection = getValues.map( _.asInstanceOf[T] ).toList
  def contents = getInput.map( _.asInstanceOf[T] ).toList
}
