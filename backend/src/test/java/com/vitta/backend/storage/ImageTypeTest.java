package com.vitta.backend.storage;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypeTest {

	@Test
	void detectsSupportedFormatsByContent() {
		assertThat(ImageType.detect(TestImages.JPEG)).contains(ImageType.JPEG);
		assertThat(ImageType.detect(TestImages.PNG)).contains(ImageType.PNG);
		assertThat(ImageType.detect(TestImages.WEBP)).contains(ImageType.WEBP);
	}

	@Test
	void rejectsOtherContent() {
		assertThat(ImageType.detect(TestImages.TEXT)).isEmpty();
		assertThat(ImageType.detect("GIF89a....".getBytes(StandardCharsets.US_ASCII))).isEmpty();
		// RIFF but not WebP (e.g. a WAV file).
		assertThat(ImageType.detect("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII))).isEmpty();
		assertThat(ImageType.detect(new byte[] { (byte) 0xFF })).isEmpty();
		assertThat(ImageType.detect(new byte[0])).isEmpty();
		assertThat(ImageType.detect(null)).isEmpty();
	}

}
