package com.vitta.backend.catalog.service;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			Conjunto Encaje Floral       | conjunto-encaje-floral
			Corpiño Señorita             | corpino-senorita
			Bombacha Tiro Alto (Algodón) | bombacha-tiro-alto-algodon
			  Body   --  Lycra!!         | body-lycra
			MEDIAS 3/4                   | medias-3-4
			Set N°2                      | set-n-2
			""")
	void slugifiesNames(String name, String expected) {
		assertThat(SlugGenerator.slugify(name, 170, "producto")).isEqualTo(expected).matches(SlugGenerator.PATTERN);
	}

	@Test
	void fallsBackWhenNothingUsableIsLeft() {
		assertThat(SlugGenerator.slugify("¡¡!!", 170, "producto")).isEqualTo("producto");
		assertThat(SlugGenerator.slugify(null, 120, "categoria")).isEqualTo("categoria");
	}

	@Test
	void truncatesLeavingRoomForSuffixWithoutTrailingHyphen() {
		String slug = SlugGenerator.slugify("abcdefg hij", 12, "x");

		assertThat(slug).isEqualTo("abcdefg");
		assertThat(slug.length()).isLessThanOrEqualTo(12 - 4);
	}

	@Test
	void keepsBaseWhenFree() {
		assertThat(SlugGenerator.unique("body", Set.of("otro")::contains)).isEqualTo("body");
	}

	@Test
	void appendsFirstFreeSuffix() {
		assertThat(SlugGenerator.unique("body", Set.of("body", "body-2", "body-3")::contains)).isEqualTo("body-4");
	}

}
