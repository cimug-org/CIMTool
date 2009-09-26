/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.cimtoole.project;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import au.com.langdale.cimtoole.CIMToolPlugin;
import au.com.langdale.cimtoole.ResourceOutputStream;
import au.com.langdale.cimtoole.builder.CIMBuilder;
import au.com.langdale.profiles.SpreadsheetParser;
import au.com.langdale.profiles.SpreadsheetParser.AssocCellSpec;
import au.com.langdale.profiles.SpreadsheetParser.AttribCellSpec;
import au.com.langdale.profiles.SpreadsheetParser.CellSpec;
import au.com.langdale.profiles.SpreadsheetParser.ClassCellSpec;
import au.com.langdale.profiles.SpreadsheetParser.ColNum;
import au.com.langdale.profiles.SpreadsheetParser.IndexNum;
import au.com.langdale.util.Logger;

import au.com.langdale.kena.OntModel;

/**
 * Adapt the SpreadsheetParser to the eclipse environment and the parameters of an ERCOT 
 * profile spreadsheet.
 * 
 */
public class SpreadsheetImporter {
	
	public static class ClassSpec extends ClassCellSpec {
		public ClassSpec() {
			sheetNo.set("1");
			firstRow.set("2");
			classCol.set("C");
			flagCol.set("G");
			nsCol.set("E");
		}
	}
	
	public static class AttribSpec extends AttribCellSpec {
		public AttribSpec() {
			sheetNo.set("2");
			firstRow.set("2");
			classCol.set("C");
			propCol.set("E");
			flagCol.set("J");
			nsCol.set("H");
		}
	}
	
	public static class AssocASpec extends AssocCellSpec {
		public AssocASpec() {
			sheetNo.set("3");
			firstRow.set("2");
			classCol.set("B");
			propCol.set("F");
			cardCol.set("H");
			flagCol.set("K");
			nsCol.set("J");
		}
	}
	
	public static class AssocBSpec extends AssocCellSpec {
		public AssocBSpec() {
			sheetNo.set("3");
			firstRow.set("2");
			classCol.set("D");
			propCol.set("E");
			cardCol.set("G");
			flagCol.set("K");
			nsCol.set("J");
		}
	}
	
	private HSSFWorkbook book;
	private String path;
	
	public String getPathName() {
		return path;
	}
	
	public static class Choice {
		public final int index;
		public final String name;
		
		public Choice(int index, String name) {
			this.index = index;
			this.name = name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	public Choice[] getRow(IndexNum sheetnum, IndexNum rownum, ColNum colnum, int count) {
		HSSFSheet sheet = book.getSheetAt(sheetnum.toInt());
		HSSFRow row = sheet.getRow(rownum.toInt());
		short colix = colnum.toShort();
		
		int last = 0;
		while( last < count && row.getCell((short)(colix + last)) != null)
			last++;
		
		Choice[] result = new Choice[last];
		for( int ix = 0; ix < last; ix++) 
			result[ix] = new Choice(ix, row.getCell((short)(colix + ix)).toString());
		
		return result;
	}
	
	
	
	public Choice[] getStandardFlags() {
		CellSpec spec = new AssocASpec();
		return getRow(spec.sheetNo, new IndexNum().set(spec.firstRow.toInt()-1), spec.flagCol, 7);
	}

	public IWorkspaceRunnable getReader(final String source) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					book = new HSSFWorkbook(new FileInputStream(source));
					path = source;
				} catch (IOException e) {
					throw Info.error("could not import spreadsheet file", e);
				}
				monitor.worked(3);
			}
		};
	}
	
	public static CellSpec[] getStandardCells(int offset) {
		CellSpec[] specs = new CellSpec[] {new ClassSpec(), new AssocASpec(), new AssocBSpec(), new AttribSpec()};
		for( int ix = 0; ix < specs.length; ix++) {
			ColNum flagCol = specs[ix].flagCol;
			flagCol.set(flagCol.toShort() + offset);
		}
		return specs;
	}
	
	public IWorkspaceRunnable getInterpreter(final IFile destin, final String namespace, final CellSpec[] specs) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				
				IFolder schema = Info.getSchemaFolder(destin.getProject());
				monitor.worked(1);
				
				OntModel background = CIMToolPlugin.getCache().getMergedOntologyWait(schema);
				monitor.worked(1);
				
				IFile log = Info.getRelated(destin, "log");
				if(log.exists())
					log.delete(false, monitor);
				
				Logger logger = new Logger(new ResourceOutputStream(log, null, true, true));
				SpreadsheetParser parser = new SpreadsheetParser(book, background, namespace, logger);
				for( int ix = 0; ix < specs.length; ix++) {
					parser.scanCells(specs[ix]);
					monitor.worked(1);
				}
				
				parser.reorganize();
				monitor.worked(1);
				
				Task.write(parser.getResult(), namespace, false, destin, "RDF/XML-ABBREV", monitor);
				monitor.worked(1);
				Info.putProperty( destin, Info.PROFILE_NAMESPACE, namespace);

				try {
					logger.close();
				} catch (IOException e) {
					throw Info.error("could not write error report", e);
				}
				
				if( logger.getErrorCount() > 0) {
					CIMBuilder.addMarker(log, "errors in spreadsheet profile " + destin.getProjectRelativePath().lastSegment());
				}
			}
		};
	}
}
