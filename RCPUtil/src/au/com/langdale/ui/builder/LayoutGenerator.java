/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.builder;
/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.widgets.FormText;

/**
 * Public version of a private jface class. 
 * The logic has been reworked to suit Templates and Assembly.
 *
 */
public class LayoutGenerator {

    private static final Point defaultSize = new Point(150, 150);
    private static final int wrapSize = 350;
    private static final GridDataFactory nonWrappingLabelData = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(false, false);

    private static boolean hasStyle(Control c, int style) {
        return (c.getStyle() & style) != 0;
    }

    private static boolean hasMethod(Control control, String name, Class[] parameterTypes) {
        Class c = control.getClass();
        try {
            return c.getMethod(name, parameterTypes) != null;
        } catch (SecurityException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Determine how to layout the given control within a Grid.
     * The layout information is returned indirectly in the form 
     * of an object that sets layout on a control.
     */
    public static GridDataFactory defaultsFor(Control control) {
        if (control instanceof Button) {
            Button button = (Button) control;

            if (hasStyle(button, SWT.CHECK)) {
                return nonWrappingLabelData.copy();
            } else {
                return GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(Geometry.max(button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true), LayoutConstants.getMinButtonSize()));
            }
        }

        if (control instanceof Composite) {
            Composite composite = (Composite) control;

            Layout theLayout = composite.getLayout();
            if (theLayout instanceof GridLayout) {
                boolean growsHorizontally = false;
                boolean growsVertically = false;

                Control[] children = composite.getChildren();
                for (int i = 0; i < children.length; i++) {
                    Control child = children[i];

                    GridData data = (GridData) child.getLayoutData();

                    if (data != null) {
                        if (data.grabExcessHorizontalSpace) {
                            growsHorizontally = true;
                        }
                        if (data.grabExcessVerticalSpace) {
                            growsVertically = true;
                        }
                    }
                }

                return GridDataFactory.fillDefaults().grab(growsHorizontally, growsVertically);
            }
        }

        Point size = control.getSize();
        boolean wrapping = hasStyle(control, SWT.WRAP);
        boolean containsText = hasMethod(control, "setText", new Class[] { String.class }) || (control instanceof FormText); 
        boolean variable = ! (control instanceof Label);
		boolean variableText = containsText && variable;
        boolean hScroll = hasStyle(control, SWT.H_SCROLL);
        boolean vScroll = hasStyle(control, SWT.V_SCROLL);
        boolean multiLine = hasStyle(control, SWT.MULTI);
 
        boolean grabHorizontal = hScroll || variableText && !wrapping || containsText && wrapping;
		boolean grabVertical =   (vScroll || variableText && multiLine) && size.y <= 0;

        int hHint, vHint, vAlign;
        
        if (grabHorizontal) {
            // For horizontally-scrollable controls, override their horizontal
            // preferred size with a constant
            if(wrapping) { 
	            // For wrapping controls, there are two cases.
            	if(containsText) 
		            // 1. For controls that contain text (like wrapping labels,
		            // read-only text boxes,
		            // etc.) override their preferred size with the preferred wrapping
		            // point and
		            // make them grab horizontal space.

            		hHint = wrapSize;
            	else
                    // 2. For non-text controls (like wrapping toolbars), assume that
                    // their non-wrapped
                    // size is best.
            		hHint = SWT.DEFAULT;
            }
            else {
                hHint = defaultSize.x;
            }
        }
        else {
        	hHint = SWT.DEFAULT;
        }

        if( size.y > 0) {
        	vHint = size.y;
        }
        else if(grabVertical) {
        	vHint = defaultSize.y;
        }
        else {
        	vHint = SWT.DEFAULT;
        }

        if (containsText && ! variableText) {
            // Heuristic for labels: Controls that contain non-wrapping read-only
            // text should be
            // center-aligned rather than fill-aligned
            vAlign = SWT.CENTER;
        }
        else {
        	vAlign = SWT.FILL;
        }
        
        return GridDataFactory.fillDefaults().grab(grabHorizontal, grabVertical).align(SWT.FILL, vAlign).hint(hHint, vHint);
    }
}
