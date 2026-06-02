/*
 * This software is Copyright 2005,2006,2007,2008 Langdale Consultants.
 * Langdale Consultants can be contacted at: http://www.langdale.com.au
 */
package au.com.langdale.easyrules.rules.utils;

import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BritishSpellingUtils {

	private static final Properties americanToBritish = new Properties();

	static {
		try (InputStream stream = BritishSpellingUtils.class
				.getResourceAsStream("AmericanToBritishMappings.properties")) {
			if (stream != null) {
				americanToBritish.load(stream);
			}
		} catch (Exception e) {
			// Optionally log error
		}
	}

	public static Optional<String> findAmericanSpelling(String input) {
		if (input == null)
			return Optional.empty();
		String lower = input.toLowerCase();
		for (String american : americanToBritish.stringPropertyNames()) {
			if (lower.contains(american.toLowerCase())) {
				return Optional.of(american);
			}
		}
		return Optional.empty();
	}

	public static boolean doesLowerCamelCaseNameContainAmericanEnglish(String name) {
		return findAmericanEnglishWithinLowerCamelCaseName(name).size() > 0;
	}

	public static boolean doesUpperCamelCaseNameContainAmericanEnglish(String name) {
		return findAmericanEnglishWithinUpperCamelCaseName(name).size() > 0;
	}

	public static List<Map.Entry<String, String>> findAmericanEnglishWithinUpperCamelCaseName(String name) {
		List<Map.Entry<String, String>> results = new ArrayList<>();
		if (name == null)
			return results;

		Matcher matcher = Pattern.compile("[A-Z][a-z0-9]*").matcher(name);
		while (matcher.find()) {
			String word = matcher.group();
			if (americanToBritish.containsKey(word.toLowerCase())) {
				results.add(new SimpleEntry<>(word,
						CIMRuleUtils.toCapitalized(americanToBritish.getProperty(word.toLowerCase()))));
			}
		}
		return results;
	}

	public static String getUpperCamelCaseNameInBritishEnglish(String name) {
		List<Map.Entry<String, String>> results = findAmericanEnglishWithinUpperCamelCaseName(name);
		if (results.size() == 0)
			return "";
		String replacement = name;
		for (Map.Entry<String, String> entry : results) {
			String americanEnglish = entry.getKey();
			String britishEnglish = entry.getValue();
			replacement = replacement.replaceAll(americanEnglish, britishEnglish);
		}
		return replacement;
	}

	public static String getLowerCamelCaseNameInBritishEnglish(String name) {
		List<Map.Entry<String, String>> results = findAmericanEnglishWithinLowerCamelCaseName(name);
		if (results.size() == 0)
			return "";
		String replacement = name;
		for (Map.Entry<String, String> entry : results) {
			String americanEnglish = entry.getKey();
			String britishEnglish = entry.getValue();
			replacement = replacement.replaceAll(americanEnglish, britishEnglish);
		}
		return replacement;
	}

	public static List<Map.Entry<String, String>> findAmericanEnglishWithinLowerCamelCaseName(String name) {
		List<Map.Entry<String, String>> results = new ArrayList<>();
		if (name == null)
			return results;

		Matcher matcher = Pattern.compile("[a-z]+|[A-Z][a-z0-9]*").matcher(name);
		while (matcher.find()) {
			String word = matcher.group();
			String key = word.toLowerCase();
			if (americanToBritish.containsKey(key)) {
				String britishEnglish = CIMRuleUtils.isCapitalized(word)
						? CIMRuleUtils.toCapitalized(americanToBritish.getProperty(key))
						: americanToBritish.getProperty(key);
				results.add(new SimpleEntry<>(word, britishEnglish));
			}
		}
		return results;
	}

	public static String getBritishSpelling(String american) {
		String british = americanToBritish.getProperty(american.toLowerCase());
		return CIMRuleUtils.isCapitalized(american) ? CIMRuleUtils.toCapitalized(british) : british;
	}

	public static List<Map.Entry<String, String>> findAmericanWordsInParagraph(String text) {
		List<Map.Entry<String, String>> results = new ArrayList<>();
		if (text == null)
			return results;
		String[] words = text.split("\\W+");
		for (String word : words) {
			String key = word.toLowerCase();
			if (americanToBritish.containsKey(key)) {
				String britishEnglish = CIMRuleUtils.isCapitalized(word)
						? CIMRuleUtils.toCapitalized(americanToBritish.getProperty(key))
						: americanToBritish.getProperty(key);
				results.add(new SimpleEntry<>(word, britishEnglish));
			}
		}
		return results;
	}

	public static List<Map.Entry<String, String>> findAmericanWordsInComment(String comment) {
		return findAmericanWordsInParagraph(comment);
	}
	
}