/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.compare;

import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import au.com.langdale.cimtoole.compare.ModelStructureCreator.Proxy;

public class ModelDiffViewer extends ContentMergeViewer {

	private static final String BUNDLE_NAME = "au.com.langdale.cimtoole.DisplayText";
	private Text leftText;
	private Text rightText;

	public ModelDiffViewer(Composite parent, CompareConfiguration cc) {
		super(SWT.NULL, ResourceBundle.getBundle(BUNDLE_NAME), cc);
		buildControl(parent);
	}

	@Override
	protected void copy(boolean leftToRight) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void createControls(Composite composite) {
		leftText = new Text(composite, SWT.MULTI);
		rightText = new Text(composite, SWT.MULTI);
	}

	@Override
	protected byte[] getContents(boolean left) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void handleResizeAncestor(int x, int y, int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void handleResizeLeftRight(int x, int y, int leftWidth,
			int centerWidth, int rightWidth, int height) {
		leftText.setBounds(x, y, leftWidth, height);
		rightText.setBounds(x+leftWidth+centerWidth, y, rightWidth, height);
	}

	@Override
	protected void updateContent(Object ancestor, Object left, Object right) {
		updateText(leftText, left);
		updateText(rightText, right);
	}

	private void updateText(Text text, Object input) {
		if(input instanceof Proxy){
			text.setText(((Proxy)input).getDescription());
			text.setEnabled(true);
			text.setEditable(false);
		}
		else {
			text.setText("");
			text.setEnabled(false);
			text.setEditable(false);
		}
	}
}
