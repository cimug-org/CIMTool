/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;
/**
 * A data model whose value is a String.
 */
public interface TextModel {
	public void setText(String value);
	public String getText();
	
	/**
	 * A concrete text model that encapsulates a String variable.
	 */
	public static class TextValue implements TextModel {
		private String value, label;
		
		public TextValue(String label) {
			this.label = label;
		}

		public String getText() {
			return value;
		}

		public void setText(String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
}
