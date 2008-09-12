/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole;

import java.util.ListResourceBundle;

public class DisplayText extends ListResourceBundle {

	@Override
	protected Object[][] getContents() {

		return new Object[][]  {
				{ "title" , "Profile Detail" },
				{ "action.CopyLeftToRight.label", "" },
				{ "action.CopyRightToLeft.label", "" }
		};
		
	}

}
