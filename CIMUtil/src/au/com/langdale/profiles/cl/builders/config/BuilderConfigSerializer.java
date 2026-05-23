/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders.config;

import au.com.langdale.profiles.cl.builders.BuilderConfig;
import au.com.langdale.profiles.cl.builders.BuilderType;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson serializer for {@link BuilderConfig}.
 * 
 * <p>
 * Serializes XSLT builders to JSON in the format expected by builders.json:
 * </p>
 * 
 * <pre>
 * {
 *     "type": "XSD",
 *     "style": "xsd",
 *     "datetime": "2020-05-01T00:00:00Z",
 *     "ext": "xsd",
 *     "includeFile": "optional-includes.xsl"
 * }
 * </pre>
 * <p>
 * Serializes Java builders to JSON in the format expected by
 * java-builders.json:
 * </p>
 * 
 * <pre>
 * {
 *     "type": "JAVA",
 *     "javaGenerator": "OWL",
 *     "format": "RDF/XML",
 *     "withInverses": false,
 *     "datetime": "2020-05-01T00:00:00Z",
 *     "ext": "simple-flat-owl"
 * }
 * </pre>
 */
public class BuilderConfigSerializer implements JsonSerializer<BuilderConfig> {

	@Override
	public JsonElement serialize(BuilderConfig config, Type type, JsonSerializationContext context) {

		JsonObject jsonObject = new JsonObject();

		jsonObject.add("type", new JsonPrimitive(config.getType().name()));

		if (config.getType() == BuilderType.JAVA) {
			// Java builder fields
			if (config.getJavaGenerator() != null) {
				jsonObject.add("javaGenerator", new JsonPrimitive(config.getJavaGenerator().name()));
			}
			if (config.getFormat() != null) {
				jsonObject.add("format", new JsonPrimitive(config.getFormat()));
			}
			jsonObject.add("withInverses", new JsonPrimitive(config.isWithInverses()));
		} else {
			// XSLT builder fields
			jsonObject.add("style", new JsonPrimitive(config.getStyle()));
		}

		jsonObject.add("datetime", new JsonPrimitive(config.getCreatedDateTime().toString()));
		jsonObject.add("ext", new JsonPrimitive(config.getExtension()));

		if (config.hasIncludesFile()) {
			jsonObject.add("includeFile", new JsonPrimitive(config.getIncludesFile()));
		}

		return jsonObject;
	}
}