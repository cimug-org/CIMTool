/*
 * This software is Copyright 2005-2024 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.profiles.cl.builders.config;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Gson deserializer for {@link java.time.Instant}.
 * 
 * Parses ISO-8601 datetime strings (e.g., "2020-05-01T00:00:00Z") into Instant objects.
 */
public class InstantDeserializer implements JsonDeserializer<Instant> {

	@Override
	public Instant deserialize(JsonElement element, Type type,
			JsonDeserializationContext context) throws JsonParseException {
		
		if (element == null || element.isJsonNull()) {
			return null;
		}
		
		try {
			String datetimeValue = element.getAsString();
			if (datetimeValue == null || datetimeValue.isEmpty()) {
				return null;
			}
			return Instant.parse(datetimeValue);
		} catch (DateTimeParseException e) {
			System.err.println("Failed to parse datetime: " + element.getAsString());
			e.printStackTrace(System.err);
			return null;
		}
	}
}