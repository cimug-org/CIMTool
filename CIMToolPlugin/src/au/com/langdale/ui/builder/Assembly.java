/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.PageBook;

import au.com.langdale.ui.plumbing.Plumbing;
import au.com.langdale.ui.util.IconCache;

/**
 *  A base class for form content and associated logic.  See the base class, Plumbing,
 *  for details of the form update/refresh logic. 
 *
 *  This class holds a collection of controls and viewers, accessible by name,
 *  and the event listeners needed to link them to the plumbing.
 *  
 *  They can be retrieved by that name with an accessor method of the appropriate type
 *  such as getText() or getButton(). The accessor methods are generally required 
 *  to implement the plumbing update() and refresh() methods.
 *  
 *  The widgets are built with a FormToolkit which determines their look and feel.  
 *  
 *  FormToolkit is an eclipse UI concept. This class also provides factory 
 *  methods for FormToolkits. 
 */
public abstract class Assembly extends Plumbing {

	private FormToolkit toolkit;
	private ScrolledForm form;
	private Map subjects;
	private Control root;

	/** 
	 * An Assembly requires a FormToolkit, that may be shared with other assemblies.
	 * The synchronous argument determines the behaviour of the assembly's plumbing.
	 */
	public Assembly(FormToolkit toolkit, boolean synchronous) {
		super(synchronous);
		this.toolkit = toolkit;
		subjects = new HashMap();
	}
	
	/**
	 * A sub-assembly is linked to the same event plumbing as its parent 
	 * and uses the same form construction toolkit.
	 * @param parent
	 */
	public Assembly(Assembly parent) {
		super(parent);
		toolkit = parent.toolkit;
		subjects = new HashMap();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if( root != null)
			root.dispose();
	};
	
	/**
	 * Create an assembly that can realise the given template
	 * and shares a controller and toolkit with the present assembly.     
	 */
	public Assembly createSubAssembly(final Template template) {
		return new Assembly(this) {
			@Override
			protected Template define() { return template;	}
		};
	}
	
	/**
	 * Create an assembly that can realise the given template with the given
	 * form construction toolkit.  The synchronous argument determines the 
	 * behaviour of the plumbing.
	 */
	public static Assembly createAssembly(FormToolkit toolkit, boolean synchronous, final Template template) {
		return new Assembly(toolkit, synchronous) {
			@Override
			protected Template define() { return template;	}
		};
	}
	
	/**
	 *  Build a widget hierarchy under the given composite
	 *  using the template associated with this assembly. 
	 *  
	 *  This method should only be called once as the
	 *  Template will hook widget events and register 
	 *  them against their names. 
	 */
	public Control realise(Composite parent) {
		root = define().realise(parent, this);
		return root;
	}

	/**
	 *  The implementation should construct and return a Template instance.
	 *  These are canned layout and widget specifications. 
	 *  They are created with the template 
	 *  methods such as Row(), Column() and Label().
	 */
	protected abstract Template define();

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
	

	/**
	 * Convenience to locate an icon and set the image of a control.
	 */
	public Control setIconForObject(String name, Object value) {
		Control widget = getControl(name);
		if( widget instanceof Button )
			((Button)widget).setImage(IconCache.get(value, 32));
		else if( widget instanceof Label)
			((Label)widget).setImage(IconCache.get(value, 32));
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
	public ArrayComposite getArrayComposite(String name) {
		return (ArrayComposite) getControl(name);
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
	
	public void setForm(ScrolledForm form) {
		this.form = form;
	}
	
	public FormToolkit getToolkit() {
		return toolkit;
	}
	
	/**
	 * Get a generic control.
	 */
	public Control getControl(String name) {
		return (Control) subjects.get(name);
	}

	/**
	 * Get a generic Viewer.
	 */
	public Viewer getViewer(String name) {
		return (Viewer) subjects.get(name);
	}

	/**
	 * Register a control under its name.
	 */
	public void putControl(String name, Control control) {
		subjects.put(name, control);
	}

	/**
	 * Register a viewer under its name.
	 */
	public void putViewer(String name, Viewer viewer) {
		subjects.put(name, viewer);
	}

	public final SelectionListener selectionListener = new SelectionListener() {

		public void widgetDefaultSelected(SelectionEvent e) {
			fireWidgetEvent();
		}

		public void widgetSelected(SelectionEvent e) {
			fireWidgetEvent();
		}
		
	};
	
	public final SelectionListener refreshSelectionListener = new SelectionListener() {

		public void widgetDefaultSelected(SelectionEvent e) {
			doRefresh();
		}

		public void widgetSelected(SelectionEvent e) {
			doRefresh();
		}
		
	};
	
	public final ModifyListener modifyListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			fireWidgetEvent();
		}
	};

	public final ISelectionChangedListener selectionChangedlistener = new ISelectionChangedListener() {

		public void selectionChanged(SelectionChangedEvent event) {
			fireWidgetEvent();
		}
		
	};
	
	public final ICheckStateListener checkStateListener = new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
			fireWidgetEvent();
		}
	};
	
	public final ICheckStateListener singleCheckedTableListener = new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
			if( event.getChecked()) {
				CheckboxTableViewer source = (CheckboxTableViewer) event.getCheckable();
				source.setCheckedElements(new Object[] {event.getElement()});
			}
			fireWidgetEvent();
		}
	};
	
	public final ICheckStateListener singleCheckedTreeListener = new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent event) {
			if( event.getChecked()) {
				CheckboxTreeViewer source = (CheckboxTreeViewer) event.getCheckable();
				source.setCheckedElements(new Object[] {event.getElement()});
			}
			fireWidgetEvent();
		}
	};
	


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
