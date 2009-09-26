package au.com.langdale.kena;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LocalNameIndex extends SearchIndex {

	private Set spaces = new HashSet();
	
	/**
	 * Index resources 
	 * @param model: the model containing the resources 
	 */
	@Override
	public void scan(OntModel model) {
		ResIterator it = model.listSubjects();
		while(it.hasNext()) {
			OntResource res = it.nextResource();
			if( res.isURIResource() ) {
				addWord(res.getLocalName());
				spaces.add(res.getNameSpace());
			}
		}
	}
	
	/**
	 * Find resources by local name.
	 * @param name: the local name
	 * @param model: the model containing the resources
	 * @return a set of <code>Resource</code>
	 */
	@Override
	public Set locate(String name, OntModel model) {
		Set result = Collections.EMPTY_SET;
		for (Iterator it = spaces.iterator(); it.hasNext();) {
			String space = (String) it.next();
			OntResource res = model.createResource(space + name);
			if(res.hasRDFType()) {
				if( result.size() == 0)
					result = Collections.singleton(res);
				else { 
					if( result.size() == 1) 
						result = new HashSet(result);
					result.add(res);
				}
			}
		}
		return result;
	}

}
