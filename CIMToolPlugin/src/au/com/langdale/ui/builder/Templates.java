/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import java.io.File;
import java.util.Collection;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.PageBook;

import au.com.langdale.ui.util.IconCache;

/**
 *  This class provides canned layout and widget specifications called templates. 
 *  They are instantiated with static methods such as Row(), Column() and Label().
 *  
 *  The widgets themselves are built when the template's realise() method is called.
 *  
 *  Layout templates such as Row() group widget templates such as Label().
 *  The latter accept a name which can be used to retrieve their SWT control or viewer
 *  object the template is realised.
 *  
 *  As a special case, the Grid() template accepts Group() instances that
 *  represent rows of the realised grid layout and in turn accept other templates.
 *
 */
public class Templates {

	private static abstract class SubjectTemplate implements Template {
		
		protected int style;
		protected String text;
		protected String name;

		SubjectTemplate(int style, String name, String text) {
			this.style = style;
			this.name = name;
			this.text = text;
		}
		
		protected void register(Control widget, Assembly assembly) {
			if(name != null)
				assembly.putControl(name, widget);
		}
	}
	
	private static abstract class ButtonTemplate extends SubjectTemplate {
		
		private Object image;

		ButtonTemplate(int style, String name, String text, Object image) {
			super(style, name, text);
			this.image = image;
		}
		
		protected abstract void listen(Button widget, Assembly assembly);
		
		public Control realise(Composite parent, Assembly assembly) {
			Button widget;
			if(image != null) {
				widget = assembly.getToolkit().createButton(parent, null, style);
				widget.setImage(IconCache.get(image));
				widget.setToolTipText(text);
			}
			else {
				widget = assembly.getToolkit().createButton(parent, text, style);
			}
				
			register(widget, assembly);
			listen(widget, assembly);
			return widget;
		}
	}

	private static class PushButtonTemplate extends ButtonTemplate {
		private SelectionListener listener;

		PushButtonTemplate(int style, String name, String text, Object image,	SelectionListener listener) {
			super(style, name, text, image);
			this.listener = listener;
		}
		
		protected void listen(Button widget, Assembly assembly) {
			widget.addSelectionListener(listener);
		}
	}

	private static class FileButtonTemplate extends ButtonTemplate {

		private String field;
		private String[] exts;
		private int style;

		FileButtonTemplate(int style, String name, String field, String[] exts	) {
			super(SWT.PUSH, name, "..", null);
			this.field = field;
			this.exts = exts;
			this.style = style;
		}
		
		protected void listen(Button widget, final Assembly assembly) {
			widget.addSelectionListener(new SelectionListener(){
				public void widgetDefaultSelected(SelectionEvent e) {}
				
				public void widgetSelected(SelectionEvent e) {
					Text entry = assembly.getText(field);
					String choice = askFile(entry.getShell(), entry.getText());
					if( choice != null && ! choice.equals(entry.getText()))
						entry.setText(choice);
				}
			});
		}
	    
		protected String askFile(Shell shell, String path) {
			File f = new File(path);
	        if (!f.exists()) {
				f = null;
			}
	        File d = askFile(shell, f);
	        if (d == null) {
				return null;
			}

	        return d.getAbsolutePath();
	    }

	    protected File askFile(Shell shell, File startingDirectory) {
			FileDialog dialog = new FileDialog(shell, style);
	        if (startingDirectory != null) {
				dialog.setFileName(startingDirectory.getPath());
			}
	        if (exts != null) {
				dialog.setFilterExtensions(exts);
			}
	        String file = dialog.open();
	        if (file != null) {
	            file = file.trim();
	            if (file.length() > 0) {
					return new File(file);
				}
	        }
	        return null;
	    }
	}
	
	private static class ViewCheckBoxTemplate extends ButtonTemplate {
		
		ViewCheckBoxTemplate(int style, String name, String text, Object image) {
			super(style, name, text, image);
		}

		protected void listen(Button widget, Assembly assembly) {
			widget.addSelectionListener(assembly.refreshSelectionListener);
		}
		
	}
	
	
	private static class CheckBoxTemplate extends ButtonTemplate {
		
		CheckBoxTemplate(int style, String name, String text, Object image) {
			super(style, name, text, image);
		}

		protected void listen(Button widget, Assembly assembly) {
			widget.addSelectionListener(assembly.selectionListener);
		}
		
	}
	
	
	private static class LabelTemplate extends SubjectTemplate {
		
		LabelTemplate(int style, String name, String text) {
			super(style, name, text);
		}

		public Control realise(Composite parent, Assembly assembly) {
			Label widget = assembly.getToolkit().createLabel(parent, text, style);
			register(widget, assembly);
			return widget;
		}
	}
	
	private static class ImageTemplate extends SubjectTemplate {
		Object image;
		
		ImageTemplate(int style, String name, Object image) {
			super(style, name, null);
			this.image = image;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Label widget = assembly.getToolkit().createLabel(parent, null, style);
			widget.setImage(IconCache.get(image));
			register(widget, assembly);
			return widget;
		}
	}
	
	private static class TextTemplate extends SubjectTemplate {
		private int lines;
		
		TextTemplate(int style, String name, String text, int lines) {
			super(style, name, text);
			this.lines = lines;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Text widget = assembly.getToolkit().createText(parent, text, style);
			if( lines > 0 ) {
				Point size = widget.getSize();
				size.y = widget.getLineHeight() * lines;
				widget.setSize(size);
			}
			register(widget, assembly);
			if((style&SWT.READ_ONLY) == 0)
				widget.addModifyListener(assembly.modifyListener);
			return widget;
		}
	}
	
	private static abstract class ViewerTemplate implements Template {

		protected int style;
		protected String name;
		protected StructuredViewer viewer;
		
		ViewerTemplate(int style, String name) {
			this.style = style;
			this.name = name;
		}
		
		public StructuredViewer getViewer() {
			return viewer;
		}
		
		protected void register(StructuredViewer viewer, Assembly assembly) {
			this.viewer = viewer;
			if(name != null)
				assembly.putViewer(name, viewer);
		}
	}
	
	private static class TreeViewerTemplate extends ViewerTemplate {
		
		TreeViewerTemplate(int style, String name) {
			super(style, name);
		}

		public Control realise(Composite parent, Assembly assembly) {
			Tree tree = assembly.getToolkit().createTree(parent, style);
			TreeViewer viewer = new TreeViewer(tree);
			register(viewer, assembly);
//			viewer.addSelectionChangedListener(selectionChangedlistener );
			return tree;
		}
	}
	
	private static class TableViewerTemplate extends ViewerTemplate {
		
		TableViewerTemplate(int style, String name) {
			super(style, name);
		}

		public Control realise(Composite parent, Assembly assembly) {
			Table table = assembly.getToolkit().createTable(parent, style);
			TableViewer viewer = new TableViewer(table);
			register(viewer, assembly);
			viewer.addSelectionChangedListener(assembly.selectionChangedlistener );
			viewer.setContentProvider(new DefaultContentProvider());
			return table;
		}
	}

	private static class CheckboxTreeViewerTemplate extends ViewerTemplate {
		
		private boolean containers;

		CheckboxTreeViewerTemplate(int style, String name, boolean containers) {
			super(style, name);
			this.containers = containers;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Tree tree = assembly.getToolkit().createTree(parent, style|SWT.CHECK);
			CheckboxTreeViewer viewer = containers? new ContainerCheckedTreeViewer(tree): new CheckboxTreeViewer(tree);
			register(viewer, assembly);
			if( (style&SWT.MULTI) != 0)
				viewer.addCheckStateListener(assembly.checkStateListener);
			else
				viewer.addCheckStateListener(assembly.singleCheckedTreeListener);
			return tree;
		}
	}
	
	private static class CheckboxTableViewerTemplate extends ViewerTemplate {
		
		CheckboxTableViewerTemplate(int style, String name) {
			super(style, name);
		}

		public Control realise(Composite parent, Assembly assembly) {
			Table table = assembly.getToolkit().createTable(parent, style|SWT.CHECK);
			CheckboxTableViewer viewer = new CheckboxTableViewer(table);
			register(viewer, assembly);
			if( (style&SWT.MULTI) != 0)
				viewer.addCheckStateListener(assembly.checkStateListener);
			else
				viewer.addCheckStateListener(assembly.singleCheckedTableListener);
			viewer.setContentProvider(new DefaultContentProvider());
			return table;
		}
	}
	
	public static class DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object input) {
			if( input instanceof Object[])
				return (Object[]) input;
			else if(input instanceof Collection) 
				return ((Collection)input).toArray();
			else if(input != null)
				return new Object[] {input};
			else
				return new Object[] {};
		}

		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}
	
	private static class FileFieldTemplate extends SubjectTemplate {
		private String[] extensions;
		
		FileFieldTemplate(int style, String name, String text, String[] exts) {
			super(style, name, text);
			extensions = exts;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Composite area = new Composite(parent, SWT.NONE);
			FileFieldEditor editor = new FileFieldEditor(name, text, area);
			editor.setFileExtensions(extensions);

			Text field = editor.getTextControl(area);
			field.addModifyListener(assembly.modifyListener);
			register(field, assembly);
			
			return area;
		}
		
	}
	
	private static class RowTemplate implements Template {
		private Template[] parts;
		private int type;

		RowTemplate(int type, Template[] parts) {
			this.parts = parts;
			this.type = type;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Composite row = assembly.getToolkit().createComposite(parent);
			mondrian.apply(row);
			RowLayout layout = new RowLayout(type);
			layout.fill = false;
			layout.pack = false;
			row.setLayout(layout);
			for(int ix = 0; ix < parts.length; ix++) {
				parts[ix].realise(row, assembly);
				//Control child = parts[ix].realise(row, assembly);
				//child.setLayoutData(new RowData());
			}
			return row;
		}
	}
	
	private static class RightAlignTemplate implements Template {
		private Template part;
		
		RightAlignTemplate(Template part) {
			this.part=part;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Composite cell = assembly.getToolkit().createComposite(parent);
			mondrian.apply(cell);
			
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			cell.setLayout(layout);
			
			Control child = part.realise(cell, assembly);
			GridData data = new GridData();
			data.horizontalAlignment = SWT.END;
			//data.verticalAlignment = SWT.END;
			data.grabExcessHorizontalSpace = true;
			//data.grabExcessVerticalSpace = false;
			child.setLayoutData(data);
			return cell;
		}
		
	}
	
	/**
	 * A sequence of templates specifying a row of a Grid.
	 *
	 */
	public static class Group {
		private Template[] parts;

		public Group(Template[] parts) {
			this.parts = parts;
		}

		/**
		 * Realise the group as a row within a Grid.  
		 * 
		 * Width specifies the number of columns in the grid.
		 * If the group is shorter than this, the last template
		 * will span the remaining columns.
		 * 
		 * Similarly, if a template in the group is followed
		 * by one or more nulls, it will span those columns.
		 * 
		 */
		public void realise(Composite parent, Assembly assembly, int width) {
			int ix = 0;
			while( ix < parts.length) {
				int iy = ix + 1;
				while( iy < parts.length && parts[iy] == null)
					iy++;
				if( iy == parts.length)
					iy = width;
				
				Control widget = parts[ix].realise(parent, assembly);
				LayoutGenerator.defaultsFor(widget).span(iy-ix, 1).applyTo(widget);
				
				ix = iy;
			}
		}
	}
	
	private static class GridTemplate implements Template {
		private Group[] parts;

		GridTemplate(Group[] parts) {
			this.parts = parts;
		}

		public Control realise(Composite parent, Assembly assembly) {
			Composite grid = assembly.getToolkit().createComposite(parent);
			mondrian.apply(grid);
			
			int width = 1;
			for(int ix = 0; ix < parts.length; ix++) 
				if(parts[ix] != null)
					width = Math.max(width, parts[ix].parts.length);

			for(int ix = 0; ix < parts.length; ix++)
				if( parts[ix] != null)
					parts[ix].realise(grid, assembly, width);
			
			GridLayoutFactory.swtDefaults().numColumns(width).applyTo(grid);
			return grid;
		}
	}

	private static class ArrayTemplate implements Template {
		
		protected int style;
		protected String name;
		protected Template part;

		ArrayTemplate(int style, String name, Template part) {
			this.style = style;
			this.name = name;
			this.part = part;
		}
		
		public Control realise(Composite parent, Assembly assembly) {
			ArrayComposite widget = new ArrayComposite(parent, style, assembly, part);
			if(name != null)
				assembly.putControl(name, widget);
			return widget;
		}
	}
	
	private static class FormTemplate implements Template {

		private Object image;
		private String title;
		private Template part;

		FormTemplate(Object image, String title, Template part) {
			super();
			this.title = title;
			this.part = part;
			this.image = image;
		}

		public Control realise(Composite parent, Assembly assembly) {
			ScrolledForm form = assembly.getToolkit().createScrolledForm(parent);
			if(title != null)
				form.setText(title);
			if(image != null)
				form.setImage(IconCache.get(image));
			assembly.setForm(form);
			Composite body = form.getBody();
			body.setLayout(new FillLayout());
			part.realise(body, assembly);
			return body;
		}
	}
	
	private static class StackTemplate implements Template {
		private Template[] parts;
		
		StackTemplate(Template[] parts) {
			this.parts = parts;
		}
		
		public Control realise(Composite parent, Assembly assembly) {
			PageBook book = new PageBook(parent, SWT.NONE);
			mondrian.apply(book);
			Control page = null;
			for (int ix = 0; ix < parts.length; ix++) {
				page = parts[ix].realise(book, assembly);
				page.setVisible(false);
			}
			book.showPage(page);
			return book;
		}
	}
	
	public static Template SaveButton(String name, String field, String ext) {
		return new FileButtonTemplate(SWT.SAVE, name, field, new String[] {ext});
	}
	
	public static Template OpenButton(String name, String field, String ext) {
		return new FileButtonTemplate(SWT.OPEN, name, field, new String[] {ext});
	}

	public static class Effect {
		public void apply(Control widget) {}
	}
	
	public static class Mondrian extends Effect {
		private int mondrian_ix;
		private int[] mondrian_colours = {SWT.COLOR_RED, SWT.COLOR_YELLOW, SWT.COLOR_GREEN, SWT.COLOR_BLUE, SWT.COLOR_CYAN };
		
		@Override
		public void apply(Control widget) {
			widget.setBackground(widget.getDisplay().getSystemColor(mondrian_colours[mondrian_ix]));
			mondrian_ix = (mondrian_ix + 1) % mondrian_colours.length; 
		}
	}

	private static Effect mondrian = new Effect();
	//Effect mondrian = new Mondrian();

	public static Template Right(Template a) {
		return new RightAlignTemplate(a); 
	}
	
	public static Template Row(Template a) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a}); 
	}
	
	public static Template Row(Template a, Template b) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b}); 
	}

	public static Template Row(Template a, Template b, Template c) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c}); 
	}

	public static Template Row(Template a, Template b, Template c, Template d) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c, d}); 
	}

	public static Template Row(Template a, Template b, Template c, Template d, Template e) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c, d, e}); 
	}

	public static Template Row(Template a, Template b, Template c, Template d, Template e, Template f) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c, d, e, f}); 
	}
	
	public static Template Row(Template a, Template b, Template c, Template d, Template e, Template f, Template g) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c, d, e, f, g}); 
	}
	
	public static Template Column(Template a, Template b) {
		return new GridTemplate(new Group[] {Group(a), Group(b)}); 
	}
	
	public static Template Column(Template a, Template b, Template c) {
		return new GridTemplate(new Group[] {Group(a), Group(b), Group(c)}); 
	}
	
	public static Template Column(Template a, Template b, Template c, Template d) {
		return new GridTemplate(new Group[] {Group(a), Group(b), Group(c), Group(d)}); 
	}
	
	public static Template Column(Template a, Template b, Template c, Template d, Template e) {
		return new GridTemplate(new Group[] {Group(a), Group(b), Group(c), Group(d), Group(e)}); 
	}
	
	public static Template Field(String name) {
		return new TextTemplate(SWT.SINGLE, name, "", 0);
	}
	
	public static Template Field(String name, String text) {
		return new TextTemplate(SWT.SINGLE, name, text, 0);
	}
	
	public static Template DisplayField(String name) {
		return new TextTemplate(SWT.SINGLE|SWT.READ_ONLY, name, "", 0);
	}
	
	public static Template TextArea(String name) {
		return new TextTemplate(SWT.MULTI|SWT.WRAP, name, "", 0);
	}
	
	public static Template TextArea(String name, int lines) {
		return new TextTemplate(SWT.MULTI|SWT.WRAP, name, "", lines);
	}
	
	public static Template DisplayArea(String name) {
		return new TextTemplate(SWT.MULTI|SWT.WRAP|SWT.READ_ONLY, name, "", 0);
	}
	
	public static Template DisplayArea(String name, int lines) {
		return new TextTemplate(SWT.MULTI|SWT.WRAP|SWT.READ_ONLY, name, "", lines);
	}
	
	public static Template Label(String text) {
		return new LabelTemplate(SWT.NONE, null, text);
	}
	
	public static Template Label(String name, String text) {
		return new LabelTemplate(SWT.NONE, name, text);
	}
	
	public static Template Image(String name, Object image) {
		return new ImageTemplate(SWT.NONE, name, image);
	}
	
	public static ViewerTemplate TreeViewer(String name) {
		return new TreeViewerTemplate(SWT.SINGLE, name);
	}
	
	public static ViewerTemplate TreeViewer(String name, boolean multiselect) {
		return new TreeViewerTemplate(multiselect? SWT.MULTI: SWT.SINGLE, name);
	}
	
	public static ViewerTemplate CheckboxTreeViewer(String name) {
		return new CheckboxTreeViewerTemplate(SWT.SINGLE, name, false);
	}
	
	public static ViewerTemplate CheckboxTreeViewer(String name, boolean multiselect) {
		return new CheckboxTreeViewerTemplate(multiselect? SWT.MULTI: SWT.SINGLE, name, false);
	}
	
	public static ViewerTemplate ContainerCheckboxTreeViewer(String name) {
		return new CheckboxTreeViewerTemplate(SWT.MULTI, name, true);
	}
	
	public static ViewerTemplate CheckboxTableViewer(String name) {
		return new CheckboxTableViewerTemplate(SWT.SINGLE, name);
	}
	
	public static ViewerTemplate CheckboxTableViewer(String name, boolean multiple) {
		int flag = multiple? SWT.MULTI: SWT.SINGLE;
		return new CheckboxTableViewerTemplate(flag, name);
	}
	
	public static ViewerTemplate TableViewer(String name) {
		return new TableViewerTemplate(SWT.SINGLE, name);
	}
	
	public static Template CheckBox(String text) {
		return new CheckBoxTemplate(SWT.CHECK, text, text, null);
	}
	
	public static Template CheckBox(String name, String text) {
		return new CheckBoxTemplate(SWT.CHECK, name, text, null);
	}
	
	public static Template CheckBox(String name, String text, Object image) {
		return new CheckBoxTemplate(SWT.CHECK, name, text, image);
	}
	
	public static Template ViewCheckBox(String name, String text) {
		return new ViewCheckBoxTemplate(SWT.CHECK, name, text, null);
	}
	
	public static Template RadioButton(String name, String text) {
		return new CheckBoxTemplate(SWT.RADIO, name, text, null);
	}
	
	public static Template PushButton(String name, String text, SelectionListener listener) {
		return new PushButtonTemplate(SWT.PUSH, name, text, null, listener);
	}
	
	public static Template PushButton(String name, String text, Object image, SelectionListener listener) {
		return new PushButtonTemplate(SWT.PUSH, name, text, image, listener);
	}
	
	public static Template FileField(String name, String text, String ext) {
		return new FileFieldTemplate(SWT.NONE, name, text, new String[] {ext});
	}
	
	public static Template FileField(String name, String text, String[] exts) {
		return new FileFieldTemplate(SWT.NONE, name, text, exts);
	}
	
	public static Template Stack(Template a, Template b) {
		return new StackTemplate(new Template[] {a, b});
	}
	
	public static Template Stack(Template a, Template b, Template c) {
		return new StackTemplate(new Template[] {a, b, c});
	}
	
	public static Template Stack(Template a, Template b, Template c, Template d) {
		return new StackTemplate(new Template[] {a, b, c, d});
	}
	
	public static Template Stack(Template a, Template b, Template c, Template d, Template e) {
		return new StackTemplate(new Template[] {a, b, c, d, e});
	}
	
	public static Template Grid(Group a) {
		return new GridTemplate(new Group[] {a});
	}
	
	public static Template Grid(Group a, Group b) {
		return new GridTemplate(new Group[] {a, b});
	}
	
	public static Template Grid(Group a, Group b, Group c) {
		return new GridTemplate(new Group[] {a, b, c});
	}
	
	public static Template Grid(Group a, Group b, Group c, Group d) {
		return new GridTemplate(new Group[] {a, b, c, d});
	}
	
	public static Template Grid(Group a, Group b, Group c, Group d, Group e) {
		return new GridTemplate(new Group[] {a, b, c, d, e});
	}
	
	public static Template Grid(Group a, Group b, Group c, Group d, Group e, Group f) {
		return new GridTemplate(new Group[] {a, b, c, d, e, f});
	}
	
	public static Template Grid(Group a, Group b, Group c, Group d, Group e, Group f, Group g) {
		return new GridTemplate(new Group[] {a, b, c, d, e, f, g});
	}
	
	public static Template Grid(Group a, Group b, Group c, Group d, Group e, Group f, Group g, Group h) {
		return new GridTemplate(new Group[] {a, b, c, d, e, f, g, h});
	}
	
	public static Group Group( Template a ) {
		return new Group(new Template[] {a});
	}
	
	public static Group Group( Template a, Template b ) {
		return new Group(new Template[] {a, b});
	}
	
	public static Group Group( Template a, Template b, Template c ) {
		return new Group(new Template[] {a, b, c});
	}
	
	public static Group Group( Template a, Template b, Template c, Template d ) {
		return new Group(new Template[] {a, b, c, d});
	}
	
	public static Group Group( Template a, Template b, Template c, Template d, Template e ) {
		return new Group(new Template[] {a, b, c, d, e});
	}
	
	public static Template Array( String name, Template elem ) {
		return new ArrayTemplate(SWT.H_SCROLL|SWT.V_SCROLL, name, elem );
	}
	
	public static Template Form(Object image, String title, Template part) {
		return new FormTemplate(image, title, part);
	}
	
	public static Template Form(String title, Template part) {
		return new FormTemplate(null, title, part);
	}
	
	public static Template Form(Template part) {
		return new FormTemplate(null, null, part);
	}
}
