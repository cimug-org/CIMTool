package au.com.langdale.profiles;

import java.util.Iterator;

import au.com.langdale.kena.OntResource;

import au.com.langdale.jena.TreeModelBase;

public class ProfileFixes extends TreeModelBase {
	
	public abstract class ProfileFix extends Node {

		@Override
		public boolean getErrorIndicator() {
			// TODO Auto-generated method stub
			return false;
		}
		
		public abstract void applyFix();
		public abstract String getProblem();
		public abstract String getProposal();
	
	}
	
	public abstract class ProfileFixSubset extends ProfileFix {
		
		@Override
		public void applyFix() {
			Iterator it = getChildren().iterator();
			while (it.hasNext()) {
				ProfileFix fix = (ProfileFix) it.next();
				fix.applyFix();
			}
		}
		
		@Override
		public String getProblem() {
			return "There are a total of " + getChildren().size() + " problems in this category.";
		}
		
		@Override
		public String getProposal() {
			return "Apply all proposed corrections";
		}
		
	}
	
	public class AllFixes extends ProfileFixSubset {

		@Override
		public OntResource getSubject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void populate() {
			add(new MissingDefinitions());
			add(new InconsistentDefinitions());
		}
		
	}
	
	public class MissingDefinitions extends ProfileFixSubset {

		@Override
		public OntResource getSubject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void populate() {
			// TODO Auto-generated method stub
			
		}

	}
	
	public class MissingDefinition extends ProfileFix {

		@Override
		public OntResource getSubject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void populate() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void applyFix() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getProblem() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getProposal() {
			// TODO Auto-generated method stub
			return null;
		}

	}
	
	public class InconsistentDefinitions extends ProfileFixSubset {

		@Override
		public OntResource getSubject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void populate() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public class InconsistentDefinition extends ProfileFix {

		@Override
		public OntResource getSubject() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void populate() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void applyFix() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getProblem() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getProposal() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
