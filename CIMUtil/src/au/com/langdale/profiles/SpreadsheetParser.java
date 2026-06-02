/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import au.com.langdale.kena.Composition;
import au.com.langdale.kena.OntModel;
import au.com.langdale.kena.OntResource;
import au.com.langdale.kena.Resource;
import au.com.langdale.kena.ResourceFactory;
import au.com.langdale.profiles.ProfileClass.PropertyInfo;
import au.com.langdale.util.Logger;
import au.com.langdale.util.NSMapper;
import au.com.langdale.xmi.UML;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
/**
 * Extract a profile model from a spreadsheet.  The POI library is used
 * to read the spreadsheet and POI classes represent the spreadsheet, worksheets,
 * rows and cells.
 * <p>
 * Usage: 
 * <ol>
 * <li>Instantiate with a spreadsheet object and a base model.
 * <li>Create a number of CellSpec objects and pass each to scanCells()
 * <li>Invoke reorganize()
 * <li>Retrieve the result.
 */
public class SpreadsheetParser {
	
	private static Resource CLASS = ResourceFactory.createResource(OWL.Class);
	private static Resource OBJECT_PROPERTY = ResourceFactory.createResource(OWL.ObjectProperty);
	private static Resource PROPERTY = ResourceFactory.createResource(RDF.Property);

	/**
	 * Represents a Row or Sheet number.
	 */
	public static class IndexNum {
		private int value;
		
		public int toInt() { 
			return value; 
		}
		public IndexNum set(int value) {
			this.value = value;
			return this;
		}
		public IndexNum set(String value) {
			this.value = Integer.parseInt(value) - 1;
			if( this.value < 0)
				throw new NumberFormatException();
			return this;
		}
		@Override
		public String toString() {
			return String.valueOf(value + 1);
		}
	}
	/**
	 * Represents a column letter.
	 */
	public static class ColNum {
		private short value;
		
		public short toShort() {
			return value;
		}
		public ColNum set(int value) {
			this.value = (short)value;
			return this;
		}
		public ColNum set(String value) {
			this.value = (short) (Integer.parseInt(value, 36) - 10);
			if( this.value < 0 || this.value >= 26 )
				throw new NumberFormatException();
			return this;
		}
		@Override
		public String toString() {
			return Integer.toString(value+10, 36);
		}
	}
	public static class CardUtils {
		
		public static String getCard(String card) {
			int min = getMinCard(card);
			int max = getMaxCard(card);
			return Integer.toString(min) + ".." + (max == Integer.MAX_VALUE ? "n" : Integer.toString(max));
		}
		
		private static int getMinCard(String card) {
			int lower = 0; // default value...
			// Determine and set min cardinality
			if (card.equals("*") || card.equals("n") || card.startsWith("0..")) {
				lower = 0;
			} else if (card.equals("1") || card.startsWith("1..")) {
				lower = 1;
			} else if (card.contains("..")) {
				// We know that the lower bounds is a numerical value > 1
				try {
					lower = Integer.parseInt(card.substring(0, card.indexOf(".")));
				} catch (Exception e) {
				}
			}
			return lower;
		}
		
		private static int getMaxCard(String card) {
			int upper = Integer.MAX_VALUE; // default value...
			// Determine and set max cardinality
			if (card.equals("1") || card.endsWith("..1")) {
				upper = 1;
			} else if (card.contains("..") && !card.endsWith("..*") && !card.endsWith("..n")) {
				// We know that the upper bounds is a numerical value > 1
				try {
					String maxCard = card.substring(card.lastIndexOf(".") + 1);
					upper = Integer.parseInt(maxCard);
				} catch (Exception e1) {
				}
			} else if (!card.contains("..") && !card.equals("*") && !card.equals("n")) {
				try {
					// We know that the upper bounds is a numerical value > 1
					upper = Integer.parseInt(card.substring(card.lastIndexOf(".") + 1));
				} catch (Exception e) {
				}
			}
			return upper;
		}
	}
	
	/**
	 * Represents a group of cells from which profile information can be extracted.
	 */
	public static abstract class CellSpec {
		public final IndexNum sheetNo = new IndexNum();
		public final IndexNum firstRow = new IndexNum();
		public final ColNum classCol = new ColNum();
		public final ColNum concreteCol = new ColNum(); // For classes
		public final ColNum descriptorCol = new ColNum(); // For classes
		public final ColNum propCol = new ColNum();
		public final ColNum flagCol = new ColNum();
		public final ColNum requiredCol = new ColNum();
		public final ColNum cardCol = new ColNum();
		public final ColNum nsCol = new ColNum();
		public final ColNum docCol = new ColNum();
		public final ColNum byRefCol = new ColNum(); // For associations
		//
		public final ColNum inversePropCol = new ColNum();
		public final ColNum inverseCardCol = new ColNum();
		public abstract void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem;
	}
	
	/**
	 * =======================================================================
	 * Legacy import columns format (v1)
	 * =======================================================================
	 */
	
	/**
	 * Represents a group of cells from which profile class definitions can be extracted.
	 */
	public static class ClassCellSpec extends CellSpec {
		@Override
		public void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem {
			if( ! isTrue(row, flagCol))
				return;
			
			context.getProfileFor(getLocalName(row, classCol));
		}
	}
	/**
	 * Represents a group of cells from which DatatypeProperty definitions can be extracted. 
	 */
	public static class AttribCellSpec extends CellSpec {
		@Override
		public void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem {
			if( ! isTrue(row, flagCol))
				return;
			
			ProfileClass profile = context.getProfileFor(getLocalName(row, classCol));
			context.addProperty(profile, getLocalName(row, propCol), PROPERTY);
		}
	}
	/**
	 * Represents a group of cells from which ObjectProperty definitions can be extracted.
	 */
	public static class AssocCellSpec extends CellSpec {
		@Override
		public void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem {
			if( ! isTrue(row, flagCol))
				return;
			
			ProfileClass profile = context.getProfileFor(getLocalName(row, classCol));
			OntResource prop = context.addProperty(profile, getLocalName(row, propCol), OBJECT_PROPERTY);
			try {
				context.setCardinality(profile, prop, getAssocCard(row, cardCol));
			} catch (ParseProblem ppe) {
				// Used as our default for an association role end cardinality cell that is empty
				context.setCardinality(profile, prop, "0..1");
				throw ppe; // re-throw for proper logging
			}
		}
	}
	
	/**
	 * =======================================================================
	 * New import columns format (v2)
	 * =======================================================================
	 */
	
	/**
	 * Represents a group of cells from which profile class definitions can be extracted.
	 */
	public static class ClassCellSpecV2 extends CellSpec {
		@Override
		public void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem {
			if( ! isTrue(row, flagCol))
				return;
			
			ProfileClass profileClass = context.getProfileFor(getLocalName(row, classCol), getComment(row, docCol));
			
			if(isTrue(row, concreteCol)) {
				profileClass.setStereotype(UML.concrete, true);
			}
			// Currently, only concrete classes can be declared as descriptors...
			if (isTrue(row, descriptorCol)) {
				if(isTrue(row, concreteCol))
					profileClass.setStereotype(UML.description, true);
			}
		}
	}
	/**
	 * Represents a group of cells from which DatatypeProperty definitions can be extracted. 
	 */
	public static class AttribCellSpecV2 extends CellSpec {
		@Override
		public void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem {
			if( ! isTrue(row, flagCol))
				return;
			
			ProfileClass profile = context.getProfileFor(getLocalName(row, classCol));
			OntResource prop = context.addProperty(profile, getLocalName(row, propCol), PROPERTY);
			context.setCardinality(profile, prop, (isTrue(row, requiredCol) ? "1..1" : "0..1"));
			prop.addComment(getComment(row, docCol), (String) null);
		}
		
	}
	/**
	 * Represents a group of cells from which ObjectProperty definitions can be extracted.
	 */
	public static class AssocCellSpecV2 extends CellSpec {
		@Override
		public void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem {
			if( ! isTrue(row, flagCol))
				return;
			
			ProfileClass profile = context.getProfileFor(getLocalName(row, classCol));
			String card;
			try {
				card = getAssocCard(row, cardCol);
				context.addProperty(profile, getLocalName(row, propCol), OBJECT_PROPERTY, CardUtils.getMinCard(card), CardUtils.getMaxCard(card));
			} catch (ParseProblem ppe) {
				// 0..1 is used as the default for an association role end cardinality cell that is empty (which throws the ParseProblem exception)
				context.addProperty(profile, getLocalName(row, propCol), OBJECT_PROPERTY, 0, 1);
				throw ppe; // re-throw for proper logging
			}
			
		}
	}
	/**
	 * An exception used internally to propagate errors to the Logger.
	 */
	public static class ParseProblem extends Exception {
		private static final long serialVersionUID = -1913955957664239443L;

		public ParseProblem(String message) {
			super(message);
		}
	}
	
	private String namespace;
	private OntModel model;

	private HSSFWorkbook book;
	private OntModel background;
	private OntModel result;
	private Map profiles = new HashMap();
	private NSMapper mapper;
	private Logger logger;
	/**
	 * Instantiate
	 * @param book: the spreadsheet
	 * @param background: the base model to which the profile will apply 
	 * @param namespace: the namespace for newly created profile defintions
	 * @param logger: destination for error messages
	 */
	public SpreadsheetParser(HSSFWorkbook book, OntModel result, OntModel background, String namespace, Logger logger) {
		this.namespace = namespace;
		this.book = book;
		this.result = result;
		this.background = background;
		mapper = new NSMapper(background);
		model = Composition.merge(result, background);
		this.logger = logger;
	}

	/**
	 * @return: the extracted profile model
	 */
	public OntModel getResult() {
		return result;
	}
	/**
	 * Scan a portion of the spreadsheet and extract profile information.
	 * @param spec: indicates the part of the spreadsheet to scan
	 * and the type of profile information expected.
	 */
	public void scanCells(CellSpec spec) {
		HSSFSheet sheet = book.getSheetAt(spec.sheetNo.toInt());
		int rownum = spec.firstRow.toInt();
		for( ;; ) {
			HSSFRow row = sheet.getRow(rownum++);
			if( row == null)
				break;
			try {
				spec.handleRow(this, row);
			} catch (ParseProblem e) {
				log(e, spec.sheetNo.toString(), new IndexNum().set(rownum).toString());
				//print(row, spec);
			}
		}
	}
	
	/**
	 * Complete the profile and a apply RDFS profile design rules. 
	 * This is called once all cells of interest have been scanned.
	 */
	public void reorganize() {
		ProfileReorganizer utility = new ProfileReorganizer(result, background, true);
		utility.run();
		result = utility.getResult();
	}
	
	private ProfileClass getProfileFor(String name) throws ParseProblem {
		String uri = namespace + name; // construct a profile class URI

		ProfileClass profile = (ProfileClass) profiles.get(uri);
		if( profile == null) {
			Resource base = mapper.map(name, CLASS); // construct a base class URI
			if( base == null )
				throw new ParseProblem("undefined class: " + name);
			OntResource clazz = model.createClass(uri);
			profile = new ProfileClass(clazz, namespace, model.createResource(base.asNode()));
			profiles.put(uri, profile);
		}
		return profile;
	}
	
	private ProfileClass getProfileFor(String name, String comment) throws ParseProblem {
		String uri = namespace + name; // construct a profile class URI

		ProfileClass profile = (ProfileClass) profiles.get(uri);
		if ( profile == null ) {
			Resource base = mapper.map(name, CLASS); // construct a base class URI
			if (base == null)
				throw new ParseProblem("undefined class: " + name);
			profile = new ProfileClass(model.createClass(uri), namespace, model.createResource(base.asNode()));
			profile.getSubject().setComment(comment, null);
			profiles.put(uri, profile);
		} else {
			if (!profile.getSubject().hasProperty(RDFS.comment)) {
				profile.getSubject().setComment(comment, null);
			}
		}
		return profile;
	}

	private OntResource addProperty(ProfileClass profile, String name, Resource type) throws ParseProblem {
		String qualified = profile.getBaseClass().getLocalName() + "." + name;
		Resource base = mapper.map(qualified, type); // construct a base property URI
		if( base == null )
			throw new ParseProblem("undefined " + type.asNode().getLocalName() + " : " + qualified);

		OntResource prop = model.createResource(base.getURI());
		profile.createAllValuesFrom(prop, new SelectionOptions());
		return prop;
	}
	
	private OntResource addProperty(ProfileClass profile, String name, Resource type, int min, int max) throws ParseProblem {
		String qualified = profile.getBaseClass().getLocalName() + "." + name;
		Resource base = mapper.map(qualified, type); // construct a base property URI
		if( base == null )
			throw new ParseProblem("undefined " + type.asNode().getLocalName() + " : " + qualified);

		OntResource prop = model.createResource(base.getURI());
		profile.createAllValuesFrom(prop, min, max);
		return prop;
	}

	private void setCardinality(ProfileClass profile, OntResource prop,	String card) {
		PropertyInfo info = profile.getPropertyInfo(prop);
		if( !card.startsWith("0"))
			info.setMinCardinality(CardUtils.getMinCard(card));
		if( !card.endsWith("n"))
			info.setMaxCardinality(CardUtils.getMaxCard(card));
	}
	
	private static boolean isTrue(HSSFRow row, ColNum colnum) {
		HSSFCell cell = row.getCell(colnum.toShort());
		if( cell == null )
			return false;
		return cell.toString().trim().equals("TRUE");
	}

	private static String getAttribCard(HSSFRow row, ColNum colnum) throws ParseProblem {
		HSSFCell cell = row.getCell(colnum.toShort());
		if ( cell == null ) {
			throw new ParseProblem(String.format("cell %d is empty [using a default attribute cardinality of 0..1]", colnum.toShort()));
		}
		String attrCard = cell.toString().trim();
		String processedCard = CardUtils.getCard(attrCard);
		// Return only a processed cardinality that is valid for attributes,
		// else throw an exception and process accordingly...
		if (processedCard.equals("0..1") || processedCard.equals("1..1")) {
			return processedCard;
		}
		throw new ParseProblem(String.format("cell %d has invalid attribute cardinality %s specified [using a default attribute cardinality of 0..1]", colnum.toShort(), attrCard));
	}
	
	private static String getAssocCard(HSSFRow row, ColNum colnum) throws ParseProblem {
		HSSFCell cell = row.getCell(colnum.toShort());
		if ( cell == null ) {
			throw new ParseProblem(String.format("cell %d is empty [using a default association role end cardinality of 0..1]", colnum.toShort()));
		}
		String assocCard = cell.toString().trim();
		return CardUtils.getCard(assocCard);
	}
	
	private static String getString(HSSFRow row, ColNum colnum) throws ParseProblem {
		HSSFCell cell = row.getCell(colnum.toShort());
		if( cell == null )
			throw new ParseProblem("cell " + colnum + " is empty");
		return cell.toString().trim();
	}
	
	private static String getLocalName(HSSFRow row, ColNum colnum) throws ParseProblem {
		String raw = getString(row, colnum);
		return raw; // TODO: process as NCNAME
	}
	
	private static String getComment(HSSFRow row, ColNum colnum) throws ParseProblem {
		HSSFCell cell = row.getCell(colnum.toShort());
		if (cell == null)
			return ""; // do not throw a ParseProbem for empty comments
		return cell.toString().trim(); // TODO: process as NCNAME
	}

	public static void print(PrintWriter out, HSSFRow row, CellSpec spec) {
		printCell(out, row, spec.nsCol);
		printCell(out, row, spec.classCol);
		printCell(out, row, spec.propCol);
		printCell(out, row, spec.cardCol);
		printCell(out, row, spec.flagCol);
		out.println();
	}
	
	public static void printCell(PrintWriter out, HSSFRow row, ColNum index) {
		if(row == null)
			return;
		
		HSSFCell cell = row.getCell(index.toShort());
		if( cell == null)
			out.print(" null");
		else
			out.print(" " + cell.toString());
	}
	
	void log(ParseProblem error, String sheet, String row) {
		logger.log("Sheet: " + sheet + " Row: " + row + " - " + error.getMessage());
	}
}
