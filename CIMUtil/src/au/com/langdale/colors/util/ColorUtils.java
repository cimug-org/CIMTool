package au.com.langdale.colors.util;

import java.util.Locale;

/**
 * Color utilities focused on hex/RGB conversions, luminance, contrast, and
 * simple transforms. All methods are null-safe and validate inputs where
 * applicable.
 */
public final class ColorUtils {

	private int RED = 0;
	private int GREEN = 1;
	private int BLUE = 2;

	private ColorUtils() {
	}

	/** Convert a hex color (e.g. #RRGGBB) to an RGB array of floats [0..1]. */
	public static float[] hexToRgb(String hexColor) {
		String hex = normalizeHex(hexColor);
		int r = Integer.parseInt(hex.substring(0, 2), 16);
		int g = Integer.parseInt(hex.substring(2, 4), 16);
		int b = Integer.parseInt(hex.substring(4, 6), 16);
		return new float[] { r / 255.0f, g / 255.0f, b / 255.0f };
	}

	/** WCAG relative luminance for an RGB array of floats [0..1]. */
	public static float luminance(float[] rgb) {
		return 0.2126f * gammaCorrect(rgb[0]) + 0.7152f * gammaCorrect(rgb[1]) + 0.0722f * gammaCorrect(rgb[2]);
	}

	/** Gamma correction helper. */
	private static float gammaCorrect(float value) {
		return (value <= 0.03928f) ? (value / 12.92f) : (float) Math.pow((value + 0.055) / 1.055, 2.4);
	}

	/** Back-compat text color by luminance threshold. */
	public static String getHexFontColor(String hexBackgroundColor) {
		float[] rgbBackground = hexToRgb(hexBackgroundColor);
		float l = luminance(rgbBackground);
		return (l < 0.5f) ? "#FFFFFF" : "#000000";
	}

	/** WCAG-based text color picker (black vs white), chooses higher contrast. */
	public static String getHexFontColorWcag(String hexBackgroundColor) {
		float[] bg = hexToRgb(hexBackgroundColor);
		double contrastWithBlack = contrastRatio(bg, new float[] { 0f, 0f, 0f });
		double contrastWithWhite = contrastRatio(bg, new float[] { 1f, 1f, 1f });
		return (contrastWithWhite > contrastWithBlack) ? "#FFFFFF" : "#000000";
	}

	/** Convert #RRGGBB to RGB object (uses existing RGB type). */
	public static int[] parseHexColor(String hexColor) {
		String hex = ensureHash(normalizeHex(hexColor));
		int red = Integer.parseInt(hex.substring(1, 3), 16);
		int green = Integer.parseInt(hex.substring(3, 5), 16);
		int blue = Integer.parseInt(hex.substring(5, 7), 16);
		return new int[] { red, green, blue };
	}

	/** Convert an RGB object to #RRGGBB. */
	public static String parseRGBColor(int[] colors) {
		if (colors == null || colors.length != 3) {
			throw new IllegalArgumentException("RGB color cannot be null");
		}
		return String.format("#%02X%02X%02X", clampInt(colors[0]), clampInt(colors[1]), clampInt(colors[2]));
	}

	/* ============================= New helpers ============================= */

	/** Validate hex: #RRGGBB, RRGGBB, #AARRGGBB, AARRGGBB. */
	public static boolean isValidHex(String hex) {
		if (hex == null)
			return false;
		String s = hex.startsWith("#") ? hex.substring(1) : hex;
		int n = s.length();
		if (n != 6 && n != 8)
			return false;
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
			if (!ok)
				return false;
		}
		return true;
	}

	/** Parse hex to RGBA floats [0..1]. If hex is RRGGBB, alpha = 1. */
	public static float[] hexToRgba(String hexColor) {
		if (!isValidHex(hexColor)) {
			throw new IllegalArgumentException("Invalid hex. Expected RRGGBB or AARRGGBB.");
		}
		String s = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
		final boolean hasAlpha = s.length() == 8;
		int idx = 0;
		int a = 0xFF;
		if (hasAlpha) {
			a = Integer.parseInt(s.substring(0, 2), 16);
			idx = 2;
		}
		int r = Integer.parseInt(s.substring(idx, idx + 2), 16);
		int g = Integer.parseInt(s.substring(idx + 2, idx + 4), 16);
		int b = Integer.parseInt(s.substring(idx + 4, idx + 6), 16);
		return new float[] { r / 255f, g / 255f, b / 255f, a / 255f };
	}

	/** WCAG contrast ratio between two hex colors. */
	public static double contrastRatio(String hexA, String hexB) {
		return contrastRatio(hexToRgb(hexA), hexToRgb(hexB));
	}

	/** WCAG contrast ratio between two RGB float triplets [0..1]. */
	public static double contrastRatio(float[] rgbA, float[] rgbB) {
		double l1 = Math.max(luminance(rgbA), luminance(rgbB));
		double l2 = Math.min(luminance(rgbA), luminance(rgbB));
		return (l1 + 0.05) / (l2 + 0.05);
	}

	/**
	 * Blend two colors by weight in [0..1]. weight=0 -> colorA, weight=1 -> colorB.
	 */
	public static String blend(String hexA, String hexB, float weight) {
		float w = clampFloat(weight);
		float[] a = hexToRgb(hexA);
		float[] b = hexToRgb(hexB);
		int r = Math.round((1 - w) * a[0] * 255f + w * b[0] * 255f);
		int g = Math.round((1 - w) * a[1] * 255f + w * b[1] * 255f);
		int bl = Math.round((1 - w) * a[2] * 255f + w * b[2] * 255f);
		return toHex(r, g, bl);
	}

	/** Lighten by percent [0..100] via blending with white. */
	public static String lighten(String hex, float percent) {
		float w = clampFloat(percent / 100f);
		return blend(hex, "#FFFFFF", w);
	}

	/** Darken by percent [0..100] via blending with black. */
	public static String darken(String hex, float percent) {
		float w = clampFloat(percent / 100f);
		return blend(hex, "#000000", w);
	}

	/** Convert normalized RGB [0..1] to #RRGGBB. */
	public static String rgbToHex(float[] rgb) {
		if (rgb == null || rgb.length < 3) {
			throw new IllegalArgumentException("rgb must be length >= 3");
		}
		int r = Math.round(clampFloat(rgb[0]) * 255f);
		int g = Math.round(clampFloat(rgb[1]) * 255f);
		int b = Math.round(clampFloat(rgb[2]) * 255f);
		return toHex(r, g, b);
	}

	/** Format to #RRGGBB with clamping. */
	public static String toHex(int r, int g, int b) {
		return String.format(Locale.ROOT, "#%02X%02X%02X", clampInt(r), clampInt(g), clampInt(b));
	}

	/* ============================= Internals ============================= */

	private static int clampInt(int v) {
		return Math.min(255, Math.max(0, v));
	}

	private static float clampFloat(float v) {
		return Math.min(1f, Math.max(0f, v));
	}

	private static String normalizeHex(String hexColor) {
		if (hexColor == null)
			throw new IllegalArgumentException("hexColor cannot be null");
		String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
		if (hex.length() != 6) {
			throw new IllegalArgumentException("Expected 6 hex digits (RRGGBB).");
		}
		if (!isValidHex(hex)) {
			throw new IllegalArgumentException("Invalid hex digits.");
		}
		return hex;
	}

	private static String ensureHash(String hex6) {
		return hex6.startsWith("#") ? hex6 : "#" + hex6;
	}
}