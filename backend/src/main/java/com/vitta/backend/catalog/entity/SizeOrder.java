package com.vitta.backend.catalog.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Logical ordering of variant sizes. Sizes are stored as free text (e.g. "S", "M", "L"), so they
 * are ranked here instead of alphabetically: letter sizes from smallest to largest first, then
 * numeric sizes ascending (e.g. "85", "90"), then anything else alphabetically.
 */
public final class SizeOrder {

	private static final List<String> LETTER_SIZES = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL");

	public static final Comparator<String> COMPARATOR = Comparator.comparingInt(SizeOrder::group)
		.thenComparingInt(SizeOrder::letterRank)
		.thenComparingLong(SizeOrder::numericValue)
		.thenComparing(SizeOrder::normalize);

	private SizeOrder() {
	}

	private static int group(String size) {
		if (letterRank(size) >= 0) {
			return 0;
		}
		return isNumeric(size) ? 1 : 2;
	}

	private static int letterRank(String size) {
		return LETTER_SIZES.indexOf(normalize(size));
	}

	private static long numericValue(String size) {
		return isNumeric(size) ? Long.parseLong(normalize(size)) : 0;
	}

	private static boolean isNumeric(String size) {
		String normalized = normalize(size);
		return !normalized.isEmpty() && normalized.length() <= 9 && normalized.chars().allMatch(Character::isDigit);
	}

	private static String normalize(String size) {
		return size.strip().toUpperCase(Locale.ROOT);
	}

}
