package com.vitta.backend.catalog.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Builds URL slugs ({@value #PATTERN}) from free text and makes them unique by appending
 * {@code -2}, {@code -3}, ...
 */
public final class SlugGenerator {

	/** Lowercase letters and digits in groups separated by single hyphens. */
	public static final String PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

	public static final String PATTERN_MESSAGE = "must be a lowercase slug (letters, digits and hyphens)";

	/** Room kept at the end of a generated slug for a uniqueness suffix such as {@code -12}. */
	private static final int SUFFIX_ROOM = 4;

	private SlugGenerator() {
	}

	/**
	 * Slug for {@code text}: accents removed (á→a, ñ→n), lowercase, anything else collapsed into
	 * single hyphens, cut to fit {@code maxLength} with room for a suffix. Falls back to
	 * {@code fallback} when nothing usable is left.
	 */
	public static String slugify(String text, int maxLength, String fallback) {
		String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
		int limit = maxLength - SUFFIX_ROOM;
		if (slug.length() > limit) {
			slug = slug.substring(0, limit).replaceAll("-+$", "");
		}
		return slug.isEmpty() ? fallback : slug;
	}

	/**
	 * {@code base} if free, otherwise the first free {@code base-2}, {@code base-3}, ...
	 */
	public static String unique(String base, Predicate<String> taken) {
		if (!taken.test(base)) {
			return base;
		}
		for (int suffix = 2;; suffix++) {
			String candidate = base + "-" + suffix;
			if (!taken.test(candidate)) {
				return candidate;
			}
		}
	}

}
