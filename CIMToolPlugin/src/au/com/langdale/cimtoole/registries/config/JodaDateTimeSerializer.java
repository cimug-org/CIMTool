package au.com.langdale.cimtoole.registries.config;

import java.lang.reflect.Type;

import org.joda.time.DateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JodaDateTimeSerializer implements JsonSerializer<DateTime> {

	@Override
	public JsonElement serialize(DateTime datetime, Type type,
			JsonSerializationContext context) {
		return new JsonPrimitive(datetime.toString());
	}
	
}