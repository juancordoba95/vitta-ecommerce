package com.vitta.backend.storage;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StoragePropertiesTest {

	private static final Path UPLOADS = Path.of("./uploads");

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "  ")
	void requiresCloudinaryUrlWhenRequired(String url) {
		assertThatIllegalArgumentException().isThrownBy(() -> new StorageProperties(url, UPLOADS, true))
			.withMessageContaining("CLOUDINARY_URL");
	}

	@Test
	void acceptsCloudinaryUrlWhenRequired() {
		assertThatCode(() -> new StorageProperties("cloudinary://key:secret@cloud", UPLOADS, true))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsLocalStorageWhenNotRequired() {
		assertThatCode(() -> new StorageProperties(null, UPLOADS, false)).doesNotThrowAnyException();
	}

}
