/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.xmi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.FrontsNode;

import au.com.langdale.kena.OntResource;

public abstract class AbstractEAProjectExtractor extends XMIModel implements EAProjectExtractor {

	protected File file;
	
	protected IDList packageIDs = new IDList(100);
	protected IDList objectIDs = new IDList(2000);
	
	private static String stereoPattern = "@STEREO;(.+?)@ENDSTEREO;";
	private static String name = "(.+?)=(.+?);";

	private static Pattern pattern = Pattern.compile(stereoPattern);
	private static Pattern namePattern = Pattern.compile(name);
	
	protected Map<String, List<String>> stereotypesMap = new HashMap<String, List<String>>();
	protected Map<Integer, List<TaggedValue>> taggedValuesMap = new HashMap<Integer, List<TaggedValue>>();
	
	public AbstractEAProjectExtractor(File file) {
		this.file = file;
	}
	
	protected class TaggedValue {
		public String name;
		public String value;
		
		public TaggedValue(String name, String value) {
			this.name = name;
			this.value = (value != null ? value.trim() : value);
		}
	}
	
	@SuppressWarnings("serial")
	protected class IDList extends ArrayList {
		
		public IDList(int size) {
			super(size);
		}

		public void putID(int index, OntResource id) {
			while( index >= size()) {
				add(null);
			}
			set(index, id);
		}
		
		public OntResource getID(int index) {
			if( index >= size() ) {
				return null;
			}
			OntResource res = (OntResource) get(index);
			return res;
		}
	}
	
	protected abstract void loadStereotypesCache() throws EAProjectExtractorException;
	protected abstract void loadTaggedValuesCache() throws EAProjectExtractorException;
	protected abstract void gatherPackageIDs() throws EAProjectExtractorException;
	protected abstract void extractPackages() throws EAProjectExtractorException;
	protected abstract void extractClasses() throws EAProjectExtractorException;
	protected abstract void extractAssociations() throws EAProjectExtractorException;
	protected abstract void extractAttributes() throws EAProjectExtractorException;
	
	protected void annotate(OntResource subject, String note) {
		if (note == null)
			return;

		note = note.trim();
		if (note.length() == 0)
			return;

		subject.addComment(note, LANG);
	}
	
	protected void addStereotypes(OntResource subject, String eaGUID) {
		if (stereotypesMap.containsKey(eaGUID)) {
			List<String> stereotypesList = stereotypesMap.get(eaGUID);
			for (String stereotypes : stereotypesList) {
				Matcher matcher = pattern.matcher(stereotypes);
				while (matcher.find()) {
					String description = matcher.group(1);
					Matcher stereoNameMatcher = namePattern.matcher(description);
					while (stereoNameMatcher.find()) {
						String groupName = stereoNameMatcher.group(1);
						if ("Name".equals(groupName)) {
							String stereoName = stereoNameMatcher.group(2);
							subject.addProperty(UML.hasStereotype, createStereotypeByName(stereoName));
						}
					}
				}
			}
		}
	}
	
	protected void addTaggedValues(OntResource subject, int objectId) {
		if (taggedValuesMap.containsKey(objectId)) {
			List<TaggedValue> taggedValuesList = taggedValuesMap.get(objectId);
			for (TaggedValue taggedValue : taggedValuesList) {
				FrontsNode property = Translator.annotationResource(taggedValue.name);
				if (subject != null && property != null && taggedValue.value != null) {
					subject.addProperty(property, taggedValue.value);
				}
			}
		}
	}

	protected Role extractProperty(String xuid, OntResource source, OntResource destin, String name, String note, String card, boolean aggregate, boolean sideA) {
		Role role = new Role();
		role.property = createObjectProperty(xuid, sideA, name);
		annotate(role.property, note);
		role.property.addIsDefinedBy(source.getIsDefinedBy()); // FIXME: the package of an association is not always that of the source class
		role.range = destin;
		role.aggregate = aggregate;
		if( card.equals("1") || card.endsWith("..1"))
			role.upper = 1;
		if( card.equals("*") || card.startsWith("0.."))
			role.lower = 0;
		else
			role.lower = 1;
		return role;
	}
	
}
