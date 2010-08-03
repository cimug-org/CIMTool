package au.com.langdale.cimtoole.registries;

public interface ModelParserRegistry {

	public static final String PARSER_REGISTRY_ID = "au.com.langdale.cimtoole.model_parser";
	
	public static ModelParserRegistry INSTANCE = new ModelParserRegistryImpl();
	
	public ModelParser[] getParsers();
	public ModelParser[] getParsersForExtension(String extension);
	public String[] getExtensions();
	public boolean hasParserForExtension(String extension);
	
}
