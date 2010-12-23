package com.cimphony.cimtoole.wizards;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class EPackageSelectionDialog extends FilteredItemsSelectionDialog {

	public EPackageSelectionDialog(Shell shell){
		super(shell);
	}
	
	@Override
	protected Control createExtendedContentArea(Composite parent) {
		// TODO Auto-generated method stub
		return null;
	}

	private static final String DIALOG_SETTINGS = EPackageSelectionDialog.class.getName()+".settings";	

	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = new DialogSettings(DIALOG_SETTINGS);
		return settings;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
			public boolean matchItem(Object item) {
				return matches((String)item);
			}
			public boolean isConsistentItem(Object item) {
				return true;
			}
		};
	}

	@Override
	protected Comparator<?> getItemsComparator() {
		Comparator<String> comparator = new Comparator<String>() {

			@Override
			public int compare(String arg0, String arg1) {
				return arg0.compareTo(arg1);
			}
		};
		return comparator;
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		progressMonitor.beginTask("Searching", EPackage.Registry.INSTANCE.size()); //$NON-NLS-1$
		Set<String> keySet = new TreeSet<String>(EPackage.Registry.INSTANCE.keySet());
		for(String entry : keySet){
			try{
			if ( EPackage.Registry.INSTANCE.getEPackage(entry)!=null){
				EPackage p = EPackage.Registry.INSTANCE.getEPackage(entry);
				if (p.getESuperPackage()==null){
					contentProvider.add(p.getName()+" ('"+p.getNsURI()+"')", itemsFilter);
				}
			}
			}catch (Exception ex){
				ex.printStackTrace();
			}
			progressMonitor.worked(1);
		}
		progressMonitor.done();

	}

	@Override
	public String getElementName(Object item) {
		return item.toString();
	}

}
