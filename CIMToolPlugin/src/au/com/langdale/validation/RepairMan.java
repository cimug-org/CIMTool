package au.com.langdale.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import au.com.langdale.inference.RepairFunctors.RepairAction;
import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.profiles.Renamer;

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
		if( renames.size() > 0 ) {
			Renamer mapper = new Renamer.URIMapper(profile, renames);
			profile = mapper.applyRenamings();
		}
		return profile;
	}
}
