package au.com.langdale.rules.utils;

import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BritishSpellingUtil {

    private static final Properties americanToBritish = new Properties();

    static {
        try (InputStream stream = BritishSpellingUtil.class.getResourceAsStream("AmericanToBritishMappings.properties")) {
            if (stream != null) {
                americanToBritish.load(stream);
            }
        } catch (Exception e) {
            // Optionally log error
        }
    }

    public static Optional<String> findAmericanSpelling(String input) {
        if (input == null) return Optional.empty();
        String lower = input.toLowerCase();
        for (String american : americanToBritish.stringPropertyNames()) {
            if (lower.contains(american.toLowerCase())) {
                return Optional.of(american);
            }
        }
        return Optional.empty();
    }

    public static List<Map.Entry<String, String>> findAmericanWordsInUpperCamelCase(String label) {
    	List<Map.Entry<String, String>> results = new ArrayList<>();
        if (label == null) 
        	return results;
        
        Matcher matcher = Pattern.compile("[A-Z][a-z0-9]*").matcher(label);
        while (matcher.find()) {
            String word = matcher.group();
            if (americanToBritish.containsKey(word.toLowerCase())) {
            	results.add(new SimpleEntry<>(word, americanToBritish.getProperty(word.toLowerCase())));
            }
        }
        return results;
    }
    
    public static List<Map.Entry<String, String>> findAmericanWordsInCamelCase(String label) {
    	List<Map.Entry<String, String>> results = new ArrayList<>();
        if (label == null) 
        	return results;;
        
        Matcher matcher = Pattern.compile("[a-z]+|[A-Z][a-z0-9]*").matcher(label);
        while (matcher.find()) {
            String word = matcher.group();
            if (americanToBritish.containsKey(word.toLowerCase())) {
            	results.add(new SimpleEntry<>(word, americanToBritish.getProperty(word.toLowerCase())));
            }
        }
        return results;
    }

    public static String getBritishSpelling(String american) {
        return americanToBritish.getProperty(american.toLowerCase());
    }
    
    public static List<Map.Entry<String, String>> findAmericanWordsInParagraph(String text) {
        List<Map.Entry<String, String>> results = new ArrayList<>();
        if (text == null) return results;

        String[] words = text.split("\\W+");
        for (String word : words) {
            String key = word.toLowerCase();
            if (americanToBritish.containsKey(key)) {
                results.add(new SimpleEntry<>(word, americanToBritish.getProperty(key)));
            }
        }
        return results;
    }

    public static List<Map.Entry<String, String>> findAmericanWordsInComment(String comment) {
        return findAmericanWordsInParagraph(comment);
    }
    
    public static void main(String[] args) {
    	String paragraph = "The color of my catalog was red.\nWhat color is my catalog?";
    	List<Map.Entry<String, String>> result = BritishSpellingUtil.findAmericanWordsInParagraph(paragraph);
    	System.out.println(result);
    	//
    	System.out.println(BritishSpellingUtil.findAmericanWordsInCamelCase("colorCatalog"));
    	
    	System.out.println(BritishSpellingUtil.findAmericanWordsInUpperCamelCase("colorCatalog"));
    }
}