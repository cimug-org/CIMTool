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

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
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
	/**
	 * Represents a group of cells from which profile information can be extracted.
	 */
	public static abstract class CellSpec {
		public final IndexNum sheetNo = new IndexNum();
		public final IndexNum firstRow = new IndexNum();
		public final ColNum classCol = new ColNum();
		public final ColNum propCol = new ColNum();
		public final ColNum flagCol = new ColNum();
		public final ColNum cardCol = new ColNum();
		public final ColNum nsCol = new ColNum();
		
		public abstract void handleRow(SpreadsheetParser context, HSSFRow row) throws ParseProblem;
	}
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
			context.setCardinality(profile, prop, getString(row, cardCol));
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
		Reorganizer utility = new Reorganizer(result, background, true);
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
			profile = new ProfileClass(model.createClass(uri), namespace,  model.createResource(base.asNode()));
			profiles.put(uri, profile);
		}
		return profile;
	}

	private OntResource addProperty(ProfileClass profile, String name, Resource type) throws ParseProblem {
		String qualified = profile.getBaseClass().getLocalName() + "." + name;
		Resource base = mapper.map(qualified, type); // construct a base property URI
		if( base == null )
			throw new ParseProblem("undefined "+ type.asNode().getLocalName() + " : " + qualified);

		OntResource prop = model.createResource(base.getURI());
		profile.createAllValuesFrom(prop, false);
		return prop;
	}

	private void setCardinality(ProfileClass profile, OntResource prop,	String card) {
		PropertyInfo info = profile.getPropertyInfo(prop);
		if( card.startsWith("1"))
			info.setMinCardinality(1);
		if( card.endsWith("1"))
			info.setMaxCardinality(1);
	}
	
	private static boolean isTrue(HSSFRow row, ColNum colnum) {
		HSSFCell cell = row.getCell(colnum.toShort());
		if( cell == null )
			return false;
		return cell.toString().trim().equals("TRUE");
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
