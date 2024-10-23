package au.com.langdale.ui.builder;

import org.eclipse.swt.graphics.RGB;

public final class ColorUtils {

	private ColorUtils() {
	}

	/**
	 * Helper method to convert a hex color (e.g. #RRGGBB) to an RGB array.
	 * 
	 * @param hexColor The color represented in hex to be converted to an RGB float
	 *                 array.
	 * @return A float array contain RGB as array elements.
	 */
	public static float[] hexToRgb(String hexColor) {
		hexColor = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
		int r = Integer.parseInt(hexColor.substring(0, 2), 16);
		int g = Integer.parseInt(hexColor.substring(2, 4), 16);
		int b = Integer.parseInt(hexColor.substring(4, 6), 16);
		return new float[] { r / 255.0f, g / 255.0f, b / 255.0f };
	}

	/**
	 * Method for gamma correction and luminance calculation.
	 * 
	 * @param rgb
	 * @return
	 */
	public static float luminance(float[] rgb) {
		return 0.2126f * gammaCorrect(rgb[0]) + 0.7152f * gammaCorrect(rgb[1]) + 0.0722f * gammaCorrect(rgb[2]);
	}

	/**
	 * Helper method containing the logic for performing gamma correction.
	 * 
	 * @param value 
	 * @return
	 */
	private static float gammaCorrect(float value) {
		return (value <= 0.03928f) ? (value / 12.92f) : (float) Math.pow((value + 0.055) / 1.055, 2.4);
	}

	/**
	 * Method to determine if text should be light or dark. Accepts a background
	 * color (could be the background color of a class, an enumeration or the 
	 * background of the canvas, etc.)
	 * 
	 * @param hexBackgroundColor The current background color for which to determine the text color for.
	 * @return Either the color white or black expressed in hex.
	 */
	public static String getHexFontColor(String hexBackgroundColor) {
		float[] rgbBackground = hexToRgb(hexBackgroundColor);
		float luminance = luminance(rgbBackground);
		// if luminance < 0.5 return hex white else return hex black
		return (luminance < 0.5) ? "#ffffff" : "#000000";
	}

	/**
	 * Helper method to convert #RRGGBB string to an RGB object
	 * 
	 * @param hexColor The hex color to be parsed/converted.
	 * @return A colore expressed as an instance of an RGB object.
	 */
	public static RGB parseHexColor(String hexColor) {
		// Ensure the string starts with '#' and has the correct length (7 chars:
		// #RRGGBB)
		if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() != 7) {
			throw new IllegalArgumentException("Invalid color format. Expected format: #RRGGBB");
		}

		try {
			// Parse the hex values for R, G, B
			int red = Integer.parseInt(hexColor.substring(1, 3), 16);
			int green = Integer.parseInt(hexColor.substring(3, 5), 16);
			int blue = Integer.parseInt(hexColor.substring(5, 7), 16);

			// Return the RGB object
			return new RGB(red, green, blue);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Failed to parse hex color: " + hexColor, e);
		}
	}

	/**
	 * Helper method to convert an RGB object to a hex color string (#RRGGBB)
	 * 
	 * @param color
	 * @return
	 */
	public static String parseRGBColor(RGB color) {
		if (color == null) {
			throw new IllegalArgumentException("RGB color cannot be null");
		}

		// Format the RGB values into a hex string
		String hex = String.format("#%02x%02x%02x", color.red, color.green, color.blue);

		// Convert to uppercase for consistency
		return hex.toUpperCase();
	}
}
