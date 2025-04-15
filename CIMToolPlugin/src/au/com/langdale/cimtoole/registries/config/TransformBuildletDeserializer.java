package au.com.langdale.cimtoole.registries.config;

import java.lang.reflect.Type;

import org.joda.time.DateTime;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.registries.TransformType;

public class TransformBuildletDeserializer<T extends TransformBuildlet> implements JsonDeserializer<T> {

	@Override
	public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();

		// Null-safe retrieval of fields
		String style = jsonObject.has("style") ? jsonObject.get("style").getAsString() : null;
		String typeValue = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
		DateTime datetime = jsonObject.has("datetime")
				? DateTime.parse(json.getAsJsonObject().get("datetime").getAsString())
				: null;
		String ext = jsonObject.has("ext") ? jsonObject.get("ext").getAsString() : null;
		String importFile = jsonObject.has("importFile") ? jsonObject.get("importFile").getAsString() : null;

		// Ensure required fields exist
		if (style == null || typeValue == null || datetime == null) {
			throw new JsonParseException("Missing required fields in JSON");
		}

		TransformBuildlet buildlet = null;
		try {
			switch (TransformType.valueOf(typeValue)) {
			case TEXT:
				buildlet = new TextBuildlet(style, ext, datetime, importFile);
				break;
			case XSD:
				buildlet = new XSDBuildlet(style, ext, datetime, importFile);
				break;
			case TRANSFORM:
				buildlet = new TransformBuildlet(style, ext, datetime, importFile);
				break;
			default:
				throw new IllegalArgumentException(
						"Error while deserializing transform builders configuration data for XSL file: " + style
								+ ".xsl");
			}
		} catch (IllegalArgumentException e) {
			throw new JsonParseException("Invalid transform type: " + typeValue, e);
		}

		return (T) buildlet;
	}

}
