/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;
/**
 * A data model whose single value is a boolean.
 */
public interface BooleanModel {

	public static final BooleanModel[] EMPTY_FLAGS = new BooleanModel[0];
	/**
	 * @return: the model value
	 */
	public boolean isTrue();

	public void setTrue(boolean flag);
	
	/**
	 * A boolean model implemented by a concrete boolean variable.
	 */
	public static class BooleanValue implements BooleanModel {
		private boolean value;
		private String label;
		
		public BooleanValue(String label) {
			this.label = label;
		}
		
		public boolean isTrue() {
			return value;
		}

		public void setTrue(boolean flag) {
			value = flag;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
}