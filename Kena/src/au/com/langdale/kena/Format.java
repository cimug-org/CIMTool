package au.com.langdale.kena;

public enum Format {
	
	N3("N3"), //
	TTL("TTL"), //
	TURTLE("TURTLE"), //
	RDF_XML("RDF/XML"), //
	RDF_XML_ABBREV("RDF/XML-ABBREV"), //
	RDF_XML_WITH_NODEIDS("RDF/XML-WITH-NODEIDS");
	
	private final String theFormat;

	Format(String theFormat) {
		this.theFormat = theFormat;
	}

	public String toFormat() {
		return theFormat;
	}
	
	public static boolean isXML(String value) {
		Format format = Format.toFormat(value);

		if (format == null)
			return false;
		
		switch (format) {
			case RDF_XML:
			case RDF_XML_ABBREV:
			case RDF_XML_WITH_NODEIDS:
				return true;
			default :
				return false;
		}
	}
	
	public static Format toFormat(String value) {
		for (Format format : Format.values()) {
			if (format.theFormat.equalsIgnoreCase(value)) {
				return format;
			}
		} 
		return null;
	}

	public String toString() {
		return theFormat;
	}
}