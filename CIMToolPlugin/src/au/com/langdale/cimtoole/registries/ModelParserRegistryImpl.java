package au.com.langdale.cimtoole.registries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class ModelParserRegistryImpl implements ModelParserRegistry {

	Map<String, ModelParser> parsers;
	Map<String, Collection<ModelParser>> parsersForExtension;
	
	ModelParserRegistryImpl(){
		parsers = new TreeMap<String, ModelParser>();
		parsersForExtension = new HashMap<String, Collection<ModelParser>>();
		
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extPoint = registry.getExtensionPoint(ModelParserRegistry.PARSER_REGISTRY_ID);

		IExtension[] pExts = extPoint.getExtensions();
		for (IExtension p : pExts){
			for (IConfigurationElement el: p.getConfigurationElements()){
				try {
					Object obj = el.createExecutableExtension("class");
					String[] extensions = el.getAttribute("fileExt").split("\\,");
					String id = el.getAttribute("id");
					if (obj instanceof ModelParser){
						ModelParser parser = (ModelParser)obj;
						parsers.put(id, parser);
						for (String s : extensions){
							s = s.trim();
							if (!parsersForExtension.containsKey(s)) parsersForExtension.put(s, new Vector<ModelParser>());
							parsersForExtension.get(s).add(parser);
						}
							
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public ModelParser[] getParsers() {
		return parsers.values().toArray(new ModelParser[parsers.values().size()]);
	}

	public ModelParser[] getParsersForExtension(String extension) {
		if (parsersForExtension.containsKey(extension))
			return parsersForExtension.get(extension).toArray(
					new ModelParser[parsersForExtension.get(extension).size()]);
		return new ModelParser[0];
	}

	public String[] getExtensions() {
		return parsersForExtension.keySet().toArray(new String[parsers.keySet().size()]);
	}


	public boolean hasParserForExtension(String extension) {
		return parsersForExtension.containsKey(extension);
	}

}
