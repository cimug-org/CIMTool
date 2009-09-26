package au.com.langdale.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import au.com.langdale.inference.RepairFunctors.RepairAction;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.ModelFactory;
import au.com.langdale.kena.OntModel;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class RepairMan {
	private Map actions = new HashMap();
	
	public void addAction( RepairAction action ) {
		actions.put(action.getProblem(), action);
	}
	
	public void removeAction( RepairAction action ) {
		if( action.equals( actions.get(action.getProblem()))) {
			actions.remove(action.getProblem());
		}
	}
	
	public boolean hasAction( RepairAction action ) {
		return action.equals( actions.get(action.getProblem()));
	}
	
	public void clear() {
		actions.clear();
	}
	
	public int size() {
		return actions.size();
	}
	
	public OntModel apply(OntModel profile) {
		Map renames = new HashMap();
		profile = Composition.copy(profile);
		
		Iterator it = actions.values().iterator();
		while( it.hasNext()) {
			RepairAction action = (RepairAction) it.next(); 
			action.repair(profile.getGraph(), renames);
		}
		if( renames.size() > 0 )
			profile = applyRenamings( profile, renames );
		
		return profile;
	}

	private OntModel applyRenamings(OntModel profile, Map renames) {
		OntModel result = ModelFactory.createMem();
		Graph graph = result.getGraph();
		Iterator it = profile.getGraph().find(Triple.ANY);
		while( it.hasNext()) {
			Triple t = (Triple) it.next();
			Node s = (Node) renames.get(t.getSubject());
			Node o = (Node) (t.getObject().isURI() ? renames.get(t.getObject()) : null);
			if( s != null || o != null)
				t = Triple.create(s != null? s: t.getSubject(), t.getPredicate(), o != null? o: t.getObject());
			graph.add(t);
		}
		return result;
	}
}
