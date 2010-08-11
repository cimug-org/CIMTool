/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.ui.binding;

import java.io.File;
import java.util.regex.Pattern;

/**
 * A set of validators to use in bindings.
 */
public class Validators {
	
	public static final Pattern NAMESPACE_REGEX = Pattern.compile("[A-Za-z]+:.*#");
	public static final Pattern NCNAME_REGEX = Pattern.compile("[A-Za-z_][A-Za-z0-9-_.]*");

	public static final Validator NONE = new Validator() {
		@Override
		public String validate(String value) {
			return null;
		}
	};
	
	public static final Validator DOUBLE = new Validator() {
		@Override
		public String validate(String value) {
			try {
			  Double.valueOf(value);
			  return null;
			}
			catch( NumberFormatException ex) {
			   return ex.getMessage();
			}
		}
	};
	
	public static final Validator INTEGER = new Validator() {
		@Override
		public String validate(String value) {
			try {
			  Integer.valueOf(value);
			  return null;
			}
			catch( NumberFormatException ex) {
			   return ex.getMessage();
			}
		}
	};
	
	public static final Validator NATURAL = new Validator() {
		@Override
		public String validate(String value) {
			try {
			  if( Integer.valueOf(value) < 0 )
				  return "value may not be negative";
			  else
			      return null;
			}
			catch( NumberFormatException ex) {
			   return ex.getMessage();
			}
		}
	};
	
	public static final Validator OptionalFileWithExt(final String ext) {
		return SimpleFile( new String[] { ext }, false, true );
	}
	
	public static final Validator OptionalFileAnyExt() {
		return SimpleFile( new String[0], false, false);
	}

	public static final Validator SimpleFile(String ext, boolean required) {
		return SimpleFile( new String[] { ext }, required, false );
	}
	
	public static final Validator SimpleFile(final String[] exts, boolean required) {
		return SimpleFile(exts, required, true);
	}
	
	public static final Validator SimpleFile(final String[] exts, final boolean required, final boolean requireExt) {
		return new Validator() {
			@Override
			public String validate(String value) {
				if( value.length() == 0 )
					if(required)
						return "A project resource name is required";
					else
						return null;
				if( value.contains("/") || value.contains("\\") || value.contains(" "))
					return "A simple project resource name with no folders is required";
				for(int ix = 0; ix < exts.length; ix++) {
					String ext = exts[ix];
					if( ext.startsWith("*."))
						ext = ext.substring(2);
					if(value.endsWith("." + ext))
						return null;
				}
				if( requireExt )
					return "A project resource name with a valid extension is required";
				if( value.contains("."))
					return "If given, the extension of the project resource must be valid";
				return null;
			}
		};
	}

	public static final Validator EXTANT_FILE = ExtantFile(true);
	public static final Validator OPTIONAL_EXTANT_FILE = ExtantFile(false);
	
	public static final Validator ExtantFile(final boolean required) {
		return new Validator() {
			@Override
			public String validate(String pathname) {
				if( pathname.length() == 0)
					if(required)
						return "A file name is required";
					else
						return null;
				File source = new File(pathname);
				if( ! source.isFile())
					return "An existing, plain file is required";
				if( ! source.canRead())
					return "The file cannot be read";
				return null;
			}
		};
	}
	
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
			if( ! NCNAME_REGEX.matcher(value).matches())
				return "A valid XML element name is required";
			return null;
		}
	};
	
	public static final Validator NAMESPACE = new Validator() {
		@Override
		public String validate(String value) {
			if( value.length() == 0 )
				return "A symbolic namespace URI is required";
			if( ! NAMESPACE_REGEX.matcher(value).matches())
				return "The namespace must begin with a scheme such as 'http:' and end with a '#'";
			return null;
		}
	};
}
