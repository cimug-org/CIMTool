package au.com.langdale.util;

import java.io.FileInputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

public class ValidateSchema {
  public static void main( String[] args) throws Exception {
	SchemaFactory parser = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
	Source source = new StreamSource( new FileInputStream(args[0]));
	try {
		parser.newSchema(source);
		System.out.println("Schema is valid.");
	} catch (Exception e) {
		System.out.println(e.getMessage());
	}
  }
}
