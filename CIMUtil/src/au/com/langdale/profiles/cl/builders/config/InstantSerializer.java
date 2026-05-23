/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders.config;

import java.lang.reflect.Type;
import java.time.Instant;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson serializer for {@link java.time.Instant}.
 * 
 * Serializes Instant objects to ISO-8601 datetime strings (e.g., "2020-05-01T00:00:00Z").
 */
public class InstantSerializer implements JsonSerializer<Instant> {

	@Override
	public JsonElement serialize(Instant instant, Type type,
			JsonSerializationContext context) {
		
		if (instant == null) {
			return JsonNull.INSTANCE;
		}
		return new JsonPrimitive(instant.toString());
	}
}