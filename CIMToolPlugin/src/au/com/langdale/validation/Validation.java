/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.validation;

import java.io.File;
/**
 * A set of validators to use in bindings.
 */
public class Validation {
	
	public static final String NAMESPACE_REGEX = "[A-Za-z]+:.*#";
	public static final String NCNAME_REGEX = "[A-Za-z_][A-Za-z0-9-_.]*";

	/**
	 * Validators implement this template.
	 * FIXME: should be an interface.
	 */
	public static abstract class Validator {
		/**
		 * Validate a value.
		 * 
		 * This method has the same contract as Binding.validate().
		 * @param value: the value to be validated
		 * @return: the validation error message or null if value is valid.
		 */
		public abstract String validate(String value);
	}
	
	public static final Validator NONE = new Validator() {
		@Override
		public String validate(String value) {
			return null;
		}
	};
	
	public static final Validator OptionalFile(final String ext) {
		return new Validator() {

			@Override
			public String validate(String value) {
				if( value.length() == 0 )
					return null;
				if( value.contains("/") || value.contains("\\"))
					return "A simple filename is required";
				if( ! value.endsWith("." + ext))
					return "Filename must end in ." + ext;
				return null;
			}
		};
	}

	public static final Validator SimpleFile(final String ext) {
		return new Validator() {
			@Override
			public String validate(String value) {
				if( value.length() == 0 )
					return "A new file name is required";
				if( value.contains("/") || value.contains("\\") || value.contains(" "))
					return "A simple filename with no folders is required";
				if( value.contains(".") && ! value.endsWith("." + ext))
					return "An extension of ." + ext + " or no extension is required";
				return null;
			}
		};
	}
	
	public static final Validator SimpleFile(final String[] exts) {
		return new Validator() {
			@Override
			public String validate(String value) {
				if( value.length() == 0 )
					return "A name for the file to be added to the workspace is required";
				if( value.contains("/") || value.contains("\\") || value.contains(" "))
					return "A simple name with no folders is required for the new file";
				for(int ix = 0; ix < exts.length; ix++) {
					String ext = exts[ix];
					if( ext.startsWith("*."))
						ext = ext.substring(2);
					if(value.endsWith("." + ext))
						return null;
				}
				return "A valid file extension is required for the new file";
			}
		};
	}

	public static final Validator OPTIONAL_FILE = new Validator() {
		@Override
		public String validate(String value) {
			if( value.length() == 0 )
				return null;
			if( value.contains("/") || value.contains("\\"))
				return "A simple filename is required";
			return null;
		}
	};
	
	public static final Validator EXTANT_FILE = new Validator() {
		@Override
		public String validate(String pathname) {
			if( pathname.length() == 0)
				return "A file name is required";
			File source = new File(pathname);
			if( ! source.isFile())
				return "An existing, plain file is required";
			if( ! source.canRead())
				return "The file cannot be read";
			return null;
		};
	};
	
	public static final Validator NEW_FILE = new Validator() {
		@Override
		public String validate(String pathname) {
			if( pathname.length() == 0)
				return "A file name is required";
			File destin = new File(pathname).getAbsoluteFile();
			if( destin.exists())
				return "File already exists";
			if( ! destin.getParentFile().canWrite())
				return "Directory is not writable";
			return null;
		};
	};

	public static final Validator NCNAME = new Validator() {
		@Override
		public String validate(String value) {
			if( value.length() == 0 )
				return "A name is required";
			if( !value.matches(NCNAME_REGEX))
				return "A valid XML element name is required";
			return null;
		}
	};
	
	public static final Validator NAMESPACE = new Validator() {
		@Override
		public String validate(String value) {
			if( value.length() == 0 )
				return "A symbolic namespace URI is required";
			if( !value.matches(NAMESPACE_REGEX))
				return "The namespace must begin with a scheme such as 'http:' and end with a '#'";
			return null;
		}
	};
}
