/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders.config;

import au.com.langdale.profiles.cl.builders.BuilderConfig;
import au.com.langdale.profiles.cl.builders.BuilderType;
import au.com.langdale.profiles.cl.builders.JavaGeneratorType;

import java.lang.reflect.Type;
import java.time.Instant;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Gson deserializer for {@link BuilderConfig}.
 * 
 * <p>Parses JSON entries from builders.json or java-builders.json. XSLT builder format:</p>
 * <pre>
 * {
 *     "type": "XSD",
 *     "style": "xsd",
 *     "datetime": "2020-05-01T00:00:00Z",
 *     "ext": "xsd",
 *     "includeFile": "optional-includes.xsl"
 * }
 * </pre>
 * <p>Java builder format:</p>
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
public class BuilderConfigDeserializer implements JsonDeserializer<BuilderConfig> {

	@Override
	public BuilderConfig deserialize(JsonElement json, Type type, 
			JsonDeserializationContext context) throws JsonParseException {
		
		if (json == null || !json.isJsonObject()) {
			throw new JsonParseException("Expected JSON object for BuilderConfig");
		}
		
		JsonObject jsonObject = json.getAsJsonObject();

		// Extract required fields
		String style = getStringField(jsonObject, "style");
		String typeValue = getStringField(jsonObject, "type");
		String ext = getStringField(jsonObject, "ext");
		
		// Extract datetime
		Instant datetime = null;
		if (jsonObject.has("datetime") && !jsonObject.get("datetime").isJsonNull()) {
			String datetimeStr = jsonObject.get("datetime").getAsString();
			if (datetimeStr != null && !datetimeStr.isEmpty()) {
				try {
					datetime = Instant.parse(datetimeStr);
				} catch (Exception e) {
					System.err.println("Warning: Could not parse datetime '" + datetimeStr + 
							"' for builder '" + style + "': " + e.getMessage());
				}
			}
		}
		
		// Extract optional XSLT includes file
		String includesFile = null;
		if (jsonObject.has("includeFile") && !jsonObject.get("includeFile").isJsonNull()) {
			includesFile = jsonObject.get("includeFile").getAsString();
		}

		// Validate required fields
		if (style == null || style.isEmpty()) {
			throw new JsonParseException("Missing required field 'style' in builder configuration");
		}
		if (typeValue == null || typeValue.isEmpty()) {
			throw new JsonParseException("Missing required field 'type' in builder configuration for: " + style);
		}
		if (ext == null || ext.isEmpty()) {
			throw new JsonParseException("Missing required field 'ext' in builder configuration for: " + style);
		}

		// Parse builder type
		BuilderType builderType = BuilderType.fromString(typeValue);
		if (builderType == null) {
			throw new JsonParseException("Invalid builder type '" + typeValue + "' for builder: " + style + 
					". Valid types are: TEXT, ASCIIDOC, XSD, TRANSFORM, JAVA");
		}

		// For JAVA builders, extract the additional fields
		if (builderType == BuilderType.JAVA) {
			String javaGeneratorValue = getStringField(jsonObject, "javaGenerator");
			if (javaGeneratorValue == null || javaGeneratorValue.isEmpty()) {
				throw new JsonParseException("Missing required field 'javaGenerator' for JAVA builder: " + style);
			}
			JavaGeneratorType javaGenerator = JavaGeneratorType.fromString(javaGeneratorValue);
			if (javaGenerator == null) {
				throw new JsonParseException("Invalid javaGenerator '" + javaGeneratorValue + "' for builder: " + style
						+ ". Valid values are: OWL, RDFS, COPY, PROFILE_SERIALIZER");
			}
			String format = getStringField(jsonObject, "format"); // null is valid for PROFILE_SERIALIZER
			boolean withInverses = false;
			if (jsonObject.has("withInverses") && !jsonObject.get("withInverses").isJsonNull()) {
				withInverses = jsonObject.get("withInverses").getAsBoolean();
			}
			return new BuilderConfig(style, builderType, ext, datetime, javaGenerator, format, withInverses);
		}

		return new BuilderConfig(style, builderType, ext, datetime, includesFile);
	}
	
	/**
	 * Safely extract a string field from a JSON object.
	 */
	private String getStringField(JsonObject obj, String fieldName) {
		if (obj.has(fieldName) && !obj.get(fieldName).isJsonNull()) {
			return obj.get(fieldName).getAsString();
		}
		return null;
	}
}