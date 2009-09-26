package au.com.langdale.kena;

import java.util.Iterator;
import java.util.Set;

public interface ResIterator extends Iterator {
	public OntResource nextResource();
	public Set toSet();
}
