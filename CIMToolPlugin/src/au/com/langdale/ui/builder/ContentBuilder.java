/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import java.io.File;
import java.util.Collection;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.PageBook;

import au.com.langdale.ui.plumbing.Plumbing;
import au.com.langdale.ui.plumbing.Template;
import au.com.langdale.ui.util.IconCache;

/**
 *  A base class for form content and associated logic.  See the base class, Plumbing,
 *  for details of the form update/refresh logic. 
 *
 *  This class adds canned layout and widget specifications. They are 
 *  instantiated with the template methods such as Row(), Column() and Label().
 *  
 *  The widgets are built with a FormToolkit, passed on construction, which determines 
 *  the look and feel.  FormToolkit is an eclipse UI concept.  This class provides
 *  factory methods for FormToolkits. 
 *  
 *  Templates other than layout templates accept a name or label.  The created widget 
 *  can be retrieved by that name with an accessor method of the appropriate type
 *  such as getText() or getButton(). The accessor methods are generally required 
 *  to implement update() and refresh(). 
 *
 */
public abstract class ContentBuilder extends Plumbing {

	private FormToolkit toolkit;
	private ScrolledForm form;

	/** 
	 * A ContentBuilder requires a FormToolkit, that may be shared with other ContentBuilders.
	 * 
	 */
	public ContentBuilder(FormToolkit toolkit, boolean synchronous) {
		super(synchronous);
		this.toolkit = toolkit;
	}

	/**
	 * Subclasses may extend this to monitor the validation message.
	 */
	@Override
	public void markInvalid(String message) {
		super.markInvalid(message);
		if( form != null )
			form.setMessage(message, IMessageProvider.NONE);
	}

	/**
	 * Subclasses may extend this to monitor the validation message.
	 */
	@Override
	public void markValid() {
		super.markValid();
		if( form != null )
			form.setMessage("", IMessageProvider.NONE);
	}

	/**
	 * Get a widget from the realised hierarchy of the indicated type and given name. 
	 */
	public Text getText(String name) {
		return (Text) getControl(name);
	}

	/**
	 * Convenience to set a widget's text in one operation.
	 */
	public Control setTextValue(String name, String value) {
		Control widget = getControl(name);
		if(value == null)
			value = "";
		if( widget instanceof Text)
			setText((Text) widget, value);
		else if(widget instanceof Label)
			setText((Label) widget, value);
		else if(widget instanceof Button)
			setText((Button) widget, value);
		return widget;
	}

	private void setText(Text widget, String value) {
		if( ! widget.getText().equals(value))
			widget.setText(value);
	}

	private void setText(Label widget, String value) {
		widget.setText(value);
		widget.getParent().layout(true);
	}

	private void setText(Button widget, String value) {
		widget.setText(value);
		widget.getParent().layout(true);
	}
	
	/**
	 * Make the named control visible along with the
	 * stack layer to which it belongs.  A Stack and its
	 * layers are created with Stack().  
	 */
	public void showStackLayer(String name) {
		showStackLayer(getControl(name));
	}

	/**
	 * Get a widget from the realised hierarchy of the indicated type and given name. 
	 */
	public Label getLabel(String name) {
		return (Label) getControl(name);
	}
	
	/**
	 * Get a widget from the realised hierarchy of the indicated type and given name. 
	 */
	public TreeViewer getTreeViewer(String name) {
		return (TreeViewer) getViewer(name);
	}
	
	/**
	 * Get a widget from the realised hierarchy of the indicated type and given name. 
	 */
	public CheckboxTreeViewer getCheckboxTreeViewer(String name) {
		return (CheckboxTreeViewer) getViewer(name);
	}
	
	
	/**
	 * Get a widget from the realised hierarchy of the indicated type and given name. 
	 */
	public CheckboxTableViewer getCheckboxTableViewer(String name) {
		return (CheckboxTableViewer) getViewer(name);
	}
	
	
	/**
	 * Get a widget from the realised hierarchy of the indicated type and given name. 
	 */
	public Button getButton(String name) {
		return (Button) getControl(name);
	}
	
	/**
	 * Convenience to set a Button widget's selected value in one operation.
	 */
	public Button setButtonValue(String name, boolean value) {
		Button widget = getButton(name);
		widget.setSelection(value);
		return widget;
	}
	
	/**
	 * Get the Form widget (if one was used) or null.
	 */
	public ScrolledForm getForm() {
		return form;
	}
	
	private SelectionListener selectionListener = new SelectionListener() {

		public void widgetDefaultSelected(SelectionEvent e) {
			fireWidgetEvent();
		}

		public void widgetSelected(SelectionEvent e) {
			fireWidgetEvent();
		}
		
	};
	
	private ModifyListener modifyListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			fireWidgetEvent();
		}
	};

	private ISelectionChangedListener selectionChangedlistener = new ISelectionChangedListener() {

		public void selectionChanged(SelectionChangedEvent event) {
			fireWidgetEvent();
		}
		
	};
	
	private ICheckStateListener checkStateListener = new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
			fireWidgetEvent();
		}
	};
	
	private ICheckStateListener singleCheckedTableListener = new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
			if( event.getChecked()) {
				CheckboxTableViewer source = (CheckboxTableViewer) event.getCheckable();
				source.setCheckedElements(new Object[] {event.getElement()});
			}
			fireWidgetEvent();
		}
	};
	
	private ICheckStateListener singleCheckedTreeListener = new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
			if( event.getChecked()) {
				CheckboxTreeViewer source = (CheckboxTreeViewer) event.getCheckable();
				source.setCheckedElements(new Object[] {event.getElement()});
			}
			fireWidgetEvent();
		}
	};
	

	private abstract class SubjectTemplate implements Template {
		
		protected int style;
		protected String text;
		protected String name;

		SubjectTemplate(int style, String name, String text) {
			this.style = style;
			this.name = name;
			this.text = text;
		}
		
		protected void register(Control widget) {
			if(name != null)
				putControl(name, widget);
		}
	}
	
	private class ButtonTemplate extends SubjectTemplate {
		
		private SelectionListener listener;
		private String image;

		ButtonTemplate(int style, String name, String text, String image, SelectionListener listener) {
			super(style, name, text);
			this.listener = listener;
			this.image = image;
		}

		public Control realise(Composite parent) {
			Button widget;
			if(image != null) {
				widget = toolkit.createButton(parent, null, style);
				widget.setImage(IconCache.get(image));
				widget.setToolTipText(text);
			}
			else {
				widget = toolkit.createButton(parent, text, style);
			}
				
			register(widget);
			if( style != SWT.PUSH)
				widget.addSelectionListener(selectionListener);
			if( listener != null)
				widget.addSelectionListener(listener);
			return widget;
		}
	}
	
	private class LabelTemplate extends SubjectTemplate {
		
		LabelTemplate(int style, String name, String text) {
			super(style, name, text);
		}

		public Control realise(Composite parent) {
			Label widget = toolkit.createLabel(parent, text, style);
			register(widget);
			return widget;
		}
	}
	
	private class ImageTemplate extends SubjectTemplate {
		
		ImageTemplate(int style, String name, String text) {
			super(style, name, text);
		}

		public Control realise(Composite parent) {
			Label widget = toolkit.createLabel(parent, null, style);
			widget.setImage(IconCache.get(text));
			register(widget);
			return widget;
		}
	}
	
	private class TextTemplate extends SubjectTemplate {
		
		TextTemplate(int style, String name, String text) {
			super(style, name, text);
		}

		public Control realise(Composite parent) {
			Text widget = toolkit.createText(parent, text, style);
			register(widget);
			if((style&SWT.READ_ONLY) == 0)
				widget.addModifyListener(modifyListener);
			return widget;
		}
	}
	
	public abstract class ViewerTemplate implements Template {

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
		
		protected void register(StructuredViewer viewer) {
			this.viewer = viewer;
			if(name != null)
				putViewer(name, viewer);
		}
	}
	
	private class TreeViewerTemplate extends ViewerTemplate {
		
		TreeViewerTemplate(int style, String name) {
			super(style, name);
		}

		public Control realise(Composite parent) {
			Tree tree = toolkit.createTree(parent, style);
			TreeViewer viewer = new TreeViewer(tree);
			register(viewer);
//			viewer.addSelectionChangedListener(selectionChangedlistener );
			return tree;
		}
	}
	
	private class TableViewerTemplate extends ViewerTemplate {
		
		TableViewerTemplate(int style, String name) {
			super(style, name);
		}

		public Control realise(Composite parent) {
			Table table = toolkit.createTable(parent, style);
			TableViewer viewer = new TableViewer(table);
			register(viewer);
			viewer.addSelectionChangedListener(selectionChangedlistener );
			viewer.setContentProvider(new DefaultContentProvider());
			return table;
		}
	}

	private class CheckboxTreeViewerTemplate extends ViewerTemplate {
		
		private boolean containers;

		CheckboxTreeViewerTemplate(int style, String name, boolean containers) {
			super(style, name);
			this.containers = containers;
		}

		public Control realise(Composite parent) {
			Tree tree = toolkit.createTree(parent, style|SWT.CHECK);
			CheckboxTreeViewer viewer = containers? new ContainerCheckedTreeViewer(tree): new CheckboxTreeViewer(tree);
			register(viewer);
			if( (style&SWT.MULTI) != 0)
				viewer.addCheckStateListener(checkStateListener);
			else
				viewer.addCheckStateListener(singleCheckedTreeListener);
			return tree;
		}
	}
	
	private class CheckboxTableViewerTemplate extends ViewerTemplate {
		
		CheckboxTableViewerTemplate(int style, String name) {
			super(style, name);
		}

		public Control realise(Composite parent) {
			Table table = toolkit.createTable(parent, style|SWT.CHECK);
			CheckboxTableViewer viewer = new CheckboxTableViewer(table);
			register(viewer);
			if( (style&SWT.MULTI) != 0)
				viewer.addCheckStateListener(checkStateListener);
			else
				viewer.addCheckStateListener(singleCheckedTableListener);
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
	
	private class FileFieldTemplate extends SubjectTemplate {
		private String[] extensions;
		
		FileFieldTemplate(int style, String name, String text, String[] exts) {
			super(style, name, text);
			extensions = exts;
		}

		public Control realise(Composite parent) {
			Composite area = new Composite(parent, SWT.NONE);
			FileFieldEditor editor = new FileFieldEditor(name, text, area);
			editor.setFileExtensions(extensions);

			Text field = editor.getTextControl(area);
			field.addModifyListener(modifyListener);
			register(field);
			
			return area;
		}
		
	}
	
	private class RowTemplate implements Template {
		private Template[] parts;
		private int type;

		RowTemplate(int type, Template[] parts) {
			this.parts = parts;
			this.type = type;
		}

		public Control realise(Composite parent) {
			Composite row = toolkit.createComposite(parent);
			mondrian.apply(row);
			RowLayout layout = new RowLayout(type);
			layout.fill = false;
			layout.pack = false;
			row.setLayout(layout);
			for(int ix = 0; ix < parts.length; ix++) {
				Control child = parts[ix].realise(row);
				//child.setLayoutData(new RowData());
			}
			return row;
		}
	}
	
	private class RightAlignTemplate implements Template {
		private Template part;
		
		RightAlignTemplate(Template part) {
			this.part=part;
		}

		public Control realise(Composite parent) {
			Composite cell = toolkit.createComposite(parent);
			mondrian.apply(cell);
			
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			cell.setLayout(layout);
			
			Control child = part.realise(cell);
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
	public class Group {
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
		public void realise(Composite parent, int width) {
			int ix = 0;
			while( ix < parts.length) {
				int iy = ix + 1;
				while( iy < parts.length && parts[iy] == null)
					iy++;
				if( iy == parts.length)
					iy = width;
				
				Control widget = parts[ix].realise(parent);
				LayoutGenerator.defaultsFor(widget).span(iy-ix, 1).applyTo(widget);
				
				ix = iy;
			}
		}
	}
	
	private class GridTemplate implements Template {
		private Group[] parts;

		GridTemplate(Group[] parts) {
			this.parts = parts;
		}

		public Control realise(Composite parent) {
			Composite grid = toolkit.createComposite(parent);
			mondrian.apply(grid);
			
			int width = 1;
			for(int ix = 0; ix < parts.length; ix++) 
				if(parts[ix] != null)
					width = Math.max(width, parts[ix].parts.length);

			for(int ix = 0; ix < parts.length; ix++)
				if( parts[ix] != null)
					parts[ix].realise(grid, width);
			
			GridLayoutFactory.swtDefaults().numColumns(width).applyTo(grid);
			return grid;
		}
	}
	
	private class FormTemplate implements Template {

		private String image;
		private String title;
		private Template part;

		FormTemplate(String image, String title, Template part) {
			super();
			this.title = title;
			this.part = part;
			this.image = image;
		}

		public Control realise(Composite parent) {
			form = toolkit.createScrolledForm(parent);
			if(title != null)
				form.setText(title);
			if(image != null)
				form.setImage(IconCache.get(image));
			Composite body = form.getBody();
			body.setLayout(new FillLayout());
			part.realise(body);
			return body;
		}
	}
	
	private class StackTemplate implements Template {
		private Template[] parts;
		
		StackTemplate(Template[] parts) {
			this.parts = parts;
		}
		
		public Control realise(Composite parent) {
			PageBook book = new PageBook(parent, SWT.NONE);
			mondrian.apply(book);
			Control page = null;
			for (int ix = 0; ix < parts.length; ix++) {
				page = parts[ix].realise(book);
				page.setVisible(false);
			}
			book.showPage(page);
			return book;
		}
	}
	
	private void showStackLayer(Control control) {
		Composite parent = control.getParent();
		if( parent instanceof PageBook) {
			PageBook book = (PageBook) parent;
			book.showPage(control);
			
			book.getParent().layout(true, true);
		}
		else if( parent != null) {
			showStackLayer(parent);
		}
	}

	public class FileFinder implements SelectionListener  {
		String field;
		private String[] extensions;
		private int style;
		
		public FileFinder(String field, String[] extensions, int style) {
			this.field = field;
			this.extensions = extensions;
			this.style = style;
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
					
		}
		
		public void widgetSelected(SelectionEvent e) {
			String choice = changePath();
			if( choice != null && ! choice.equals(getField().getText()))
				getField().setText(choice);
		}

		public Text getField() {
			return getText(field);
		}
	    
		protected String changePath() {
	        File f = new File(getField().getText());
	        if (!f.exists()) {
				f = null;
			}
	        File d = getFile(f);
	        if (d == null) {
				return null;
			}

	        return d.getAbsolutePath();
	    }

	    private File getFile(File startingDirectory) {

	        FileDialog dialog = new FileDialog(getField().getShell(), style);
	        if (startingDirectory != null) {
				dialog.setFileName(startingDirectory.getPath());
			}
	        if (extensions != null) {
				dialog.setFilterExtensions(extensions);
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
	
	public Template SaveButton(String name, String field, String ext) {
		FileFinder listener = new FileFinder(field, new String[] {ext}, SWT.SAVE);
		return new ButtonTemplate(SWT.PUSH, name, "Browse..", null, listener);
	}
	
	public Template OpenButton(String name, String field, String ext) {
		FileFinder listener = new FileFinder(field, new String[] {ext}, SWT.OPEN);
		return new ButtonTemplate(SWT.PUSH, name, "..", null, listener);
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

	Effect mondrian = new Effect();
	//Effect mondrian = new Mondrian();

	public Template Right(Template a) {
		return new RightAlignTemplate(a); 
	}
	
	public Template Row(Template a) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a}); 
	}
	
	public Template Row(Template a, Template b) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b}); 
	}

	public Template Row(Template a, Template b, Template c) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c}); 
	}

	public Template Row(Template a, Template b, Template c, Template d) {
		return new RowTemplate(SWT.HORIZONTAL, new Template[] {a, b, c, d}); 
	}
	
	public Template Column(Template a, Template b) {
		return new GridTemplate(new Group[] {Group(a), Group(b)}); 
	}
	
	public Template Column(Template a, Template b, Template c) {
		return new GridTemplate(new Group[] {Group(a), Group(b), Group(c)}); 
	}
	
	public Template Column(Template a, Template b, Template c, Template d) {
		return new GridTemplate(new Group[] {Group(a), Group(b), Group(c), Group(d)}); 
	}
	
	public Template Column(Template a, Template b, Template c, Template d, Template e) {
		return new GridTemplate(new Group[] {Group(a), Group(b), Group(c), Group(d), Group(e)}); 
	}
	
	public Template Field(String name) {
		return new TextTemplate(SWT.SINGLE, name, "");
	}
	
	public Template Field(String name, String text) {
		return new TextTemplate(SWT.SINGLE, name, text);
	}
	
	public Template DisplayField(String name) {
		return new TextTemplate(SWT.SINGLE|SWT.READ_ONLY, name, "");
	}
	
	public Template TextArea(String name) {
		return new TextTemplate(SWT.MULTI|SWT.WRAP, name, "");
	}
	
	public Template DisplayArea(String name) {
		return new TextTemplate(SWT.MULTI|SWT.WRAP|SWT.READ_ONLY, name, "");
	}
	
	public Template Label(String text) {
		return new LabelTemplate(SWT.NONE, null, text);
	}
	
	public Template Label(String name, String text) {
		return new LabelTemplate(SWT.NONE, name, text);
	}
	
	public Template Image(String name, String image) {
		return new ImageTemplate(SWT.NONE, name, image);
	}
	
	public ViewerTemplate TreeViewer(String name) {
		return new TreeViewerTemplate(SWT.SINGLE, name);
	}
	
	public ViewerTemplate TreeViewer(String name, boolean multiselect) {
		return new TreeViewerTemplate(multiselect? SWT.MULTI: SWT.SINGLE, name);
	}
	
	public ViewerTemplate CheckboxTreeViewer(String name) {
		return new CheckboxTreeViewerTemplate(SWT.SINGLE, name, false);
	}
	
	public ViewerTemplate CheckboxTreeViewer(String name, boolean multiselect) {
		return new CheckboxTreeViewerTemplate(multiselect? SWT.MULTI: SWT.SINGLE, name, false);
	}
	
	public ViewerTemplate ContainerCheckboxTreeViewer(String name) {
		return new CheckboxTreeViewerTemplate(SWT.MULTI, name, true);
	}
	
	public ViewerTemplate CheckboxTableViewer(String name) {
		return new CheckboxTableViewerTemplate(SWT.SINGLE, name);
	}
	
	public ViewerTemplate CheckboxTableViewer(String name, boolean multiple) {
		int flag = multiple? SWT.MULTI: SWT.SINGLE;
		return new CheckboxTableViewerTemplate(flag, name);
	}
	
	public ViewerTemplate TableViewer(String name) {
		return new TableViewerTemplate(SWT.SINGLE, name);
	}
	
	public Template CheckBox(String text) {
		return new ButtonTemplate(SWT.CHECK, text, text, null, null);
	}
	
	public Template CheckBox(String name, String text) {
		return new ButtonTemplate(SWT.CHECK, name, text, null, null);
	}
	
	public Template RadioButton(String name, String text) {
		return new ButtonTemplate(SWT.RADIO, name, text, null, null);
	}
	
	public Template PushButton(String name, String text, SelectionListener listener) {
		return new ButtonTemplate(SWT.PUSH, name, text, null, listener);
	}
	
	public Template PushButton(String name, String text, String image, SelectionListener listener) {
		return new ButtonTemplate(SWT.PUSH, name, text, image, listener);
	}
	
	public Template FileField(String name, String text, String ext) {
		return new FileFieldTemplate(SWT.NONE, name, text, new String[] {ext});
	}
	
	public Template FileField(String name, String text, String[] exts) {
		return new FileFieldTemplate(SWT.NONE, name, text, exts);
	}
	
	public Template Stack(Template a, Template b) {
		return new StackTemplate(new Template[] {a, b});
	}
	
	public Template Stack(Template a, Template b, Template c) {
		return new StackTemplate(new Template[] {a, b, c});
	}
	
	public Template Stack(Template a, Template b, Template c, Template d) {
		return new StackTemplate(new Template[] {a, b, c, d});
	}
	
	public Template Stack(Template a, Template b, Template c, Template d, Template e) {
		return new StackTemplate(new Template[] {a, b, c, d, e});
	}
	
	public Template Grid(Group a) {
		return new GridTemplate(new Group[] {a});
	}
	
	public Template Grid(Group a, Group b) {
		return new GridTemplate(new Group[] {a, b});
	}
	
	public Template Grid(Group a, Group b, Group c) {
		return new GridTemplate(new Group[] {a, b, c});
	}
	
	public Template Grid(Group a, Group b, Group c, Group d) {
		return new GridTemplate(new Group[] {a, b, c, d});
	}
	
	public Template Grid(Group a, Group b, Group c, Group d, Group e) {
		return new GridTemplate(new Group[] {a, b, c, d, e});
	}
	
	public Template Grid(Group a, Group b, Group c, Group d, Group e, Group f) {
		return new GridTemplate(new Group[] {a, b, c, d, e, f});
	}
	
	public Template Grid(Group a, Group b, Group c, Group d, Group e, Group f, Group g) {
		return new GridTemplate(new Group[] {a, b, c, d, e, f, g});
	}
	
	public Template Grid(Group a, Group b, Group c, Group d, Group e, Group f, Group g, Group h) {
		return new GridTemplate(new Group[] {a, b, c, d, e, f, g, h});
	}
	
	public Group Group( Template a ) {
		return new Group(new Template[] {a});
	}
	
	public Group Group( Template a, Template b ) {
		return new Group(new Template[] {a, b});
	}
	
	public Group Group( Template a, Template b, Template c ) {
		return new Group(new Template[] {a, b, c});
	}
	
	public Group Group( Template a, Template b, Template c, Template d ) {
		return new Group(new Template[] {a, b, c, d});
	}
	
	public Group Group( Template a, Template b, Template c, Template d, Template e ) {
		return new Group(new Template[] {a, b, c, d, e});
	}
	
	public Template Form(String image, String title, Template part) {
		return new FormTemplate(image, title, part);
	}
	
	public Template Form(String title, Template part) {
		return new FormTemplate(null, title, part);
	}
	
	public Template Form(Template part) {
		return new FormTemplate(null, null, part);
	}

	/**
	 * Create a form toolkit that uses default dialog background colour.
	 */
	public static FormToolkit createDialogToolkit() {
		FormToolkit toolkit = createFormToolkit();
		toolkit.setBackground(null);
		return toolkit; 
	}

	/**
	 * Create a form toolkit that uses form background colour.
	 */
	public static FormToolkit createFormToolkit() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		FormToolkit toolkit = new FormToolkit(display);
		toolkit.setBorderStyle(SWT.BORDER);
		return toolkit;
	}
}
