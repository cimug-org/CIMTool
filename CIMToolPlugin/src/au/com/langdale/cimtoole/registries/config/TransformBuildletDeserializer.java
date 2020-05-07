package au.com.langdale.cimtoole.registries.config;

import java.lang.reflect.Type;

import org.joda.time.DateTime;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.JSONBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TextBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.builder.ProfileBuildlets.XSDBuildlet;
import au.com.langdale.cimtoole.registries.TransformType;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class TransformBuildletDeserializer<T extends TransformBuildlet> implements JsonDeserializer<T> {

	@Override
	public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {

		String style = json.getAsJsonObject().get("style").getAsString();
		String typeValue = json.getAsJsonObject().get("type").getAsString();
		DateTime datetime = DateTime.parse(json.getAsJsonObject().get("datetime").getAsString());
		String ext = json.getAsJsonObject().get("ext").getAsString();

		TransformBuildlet buildlet = null;
		switch (TransformType.valueOf(typeValue)) {
		case JSON:
			buildlet = new JSONBuildlet(style, ext, datetime);
			break;
		case TEXT:
			buildlet = new TextBuildlet(style, ext, datetime);
			break;
		case XSD:
			buildlet = new XSDBuildlet(style, ext, datetime);
			break;
		case TRANSFORM:
			buildlet = new TransformBuildlet(style, ext, datetime);
			break;
		default:
			throw new RuntimeException("Error while deserializing transform builders configuration data for XSL file: " + style + ".xsl");
		}

		return (T) buildlet;
	}

}