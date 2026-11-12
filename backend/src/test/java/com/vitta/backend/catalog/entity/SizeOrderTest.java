package com.vitta.backend.catalog.entity;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SizeOrderTest {

	@Test
	void sortsLetterSizesFromSmallestToLargest() {
		assertThat(sorted("XL", "S", "L", "XXS", "M", "XS", "XXL")).containsExactly("XXS", "XS", "S", "M", "L", "XL",
				"XXL");
	}

	@Test
	void ignoresCaseAndSurroundingWhitespace() {
		assertThat(sorted("l", " m ", "s")).containsExactly("s", " m ", "l");
	}

	@Test
	void sortsNumericSizesNumericallyAfterLetterSizes() {
		assertThat(sorted("100", "90", "M", "85")).containsExactly("M", "85", "90", "100");
	}

	@Test
	void putsUnknownSizesLastAlphabetically() {
		assertThat(sorted("Unico", "L", "95B", "40")).containsExactly("L", "40", "95B", "Unico");
	}

	private static List<String> sorted(String... sizes) {
		return Stream.of(sizes).sorted(SizeOrder.COMPARATOR).toList();
	}

}
