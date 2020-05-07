package au.com.langdale.cimtoole.registries.config;

import java.lang.reflect.Type;

import org.joda.time.DateTime;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class JodaDateTimeDeserializer implements JsonDeserializer<DateTime> {

	@Override
	public DateTime deserialize(JsonElement element, Type type,
			JsonDeserializationContext context) throws JsonParseException {
		try {
			String datetimeValue = element.getAsString();
			return DateTime.parse(datetimeValue);
		} catch (Exception e) {
			System.err.println("Failed to parse DateTime due to:");
			e.printStackTrace(System.err);
			return null;
		}
	}
	
}