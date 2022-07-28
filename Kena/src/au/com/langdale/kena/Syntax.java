package au.com.langdale.kena;

public enum Syntax {
	
	N3("N3"), //
	TTL("TTL"), //
	TURTLE("TURTLE"), //
	RDF_XML("RDF/XML"), //
	RDF_XML_ABBREV("RDF/XML-ABBREV"), //
	RDF_XML_WITH_NODEIDS("RDF/XML-WITH-NODEIDS");

	private final String theFormat;

	Syntax(String theFormat) {
		this.theFormat = theFormat;
	}

	public String toFormat() {
		return theFormat;
	}

	public String toString() {
		return theFormat;
	}
}