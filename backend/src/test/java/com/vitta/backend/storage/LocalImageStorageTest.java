package com.vitta.backend.storage;

import java.nio.file.Files;
import java.nio.file.Path;

import com.vitta.backend.storage.ImageStorage.StoredImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LocalImageStorageTest {

	@TempDir
	private Path dir;

	@Test
	void storesUnderProductsFolderAndDeletes() throws Exception {
		LocalImageStorage storage = new LocalImageStorage(dir);

		StoredImage stored = storage.store(TestImages.PNG, ImageType.PNG);

		assertThat(stored.key()).startsWith("products/").endsWith(".png");
		assertThat(stored.url()).isEqualTo("/uploads/" + stored.key());
		Path file = dir.resolve(stored.key());
		assertThat(Files.readAllBytes(file)).isEqualTo(TestImages.PNG);

		storage.delete(stored.key());
		assertThat(file).doesNotExist();
	}

	@Test
	void deletingUnknownKeyIsIgnored() {
		new LocalImageStorage(dir).delete("products/missing.png");
	}

	@Test
	void rejectsKeysOutsideTheUploadsDirectory() {
		LocalImageStorage storage = new LocalImageStorage(dir.resolve("uploads"));

		assertThatIllegalArgumentException().isThrownBy(() -> storage.delete("../secret.txt"));
	}

}
