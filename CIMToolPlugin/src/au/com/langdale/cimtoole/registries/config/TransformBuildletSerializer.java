package au.com.langdale.cimtoole.registries.config;

import java.lang.reflect.Type;

import au.com.langdale.cimtoole.builder.ProfileBuildlets.TransformBuildlet;
import au.com.langdale.cimtoole.registries.TransformType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TransformBuildletSerializer<T extends TransformBuildlet> implements JsonSerializer<T> {

	@Override
	public JsonElement serialize(T buildlet, Type type,
			JsonSerializationContext context) {
		
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("style", new JsonPrimitive(buildlet.getStyle()));
		jsonObject.add("type", new JsonPrimitive(TransformType.toTransformType(buildlet).name()));
		jsonObject.add("datetime", new JsonPrimitive(buildlet.getDateTimeCreated().toString()));
		jsonObject.add("ext", new JsonPrimitive(buildlet.getFileExt()));
		
		return jsonObject;
	}

}