package com.vitta.backend.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Minimal byte sequences that pass (or fail) {@link ImageType#detect}. */
public final class TestImages {

	public static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H',
			'D', 'R' };

	public static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I',
			'F' };

	public static final byte[] WEBP = concat("RIFF".getBytes(StandardCharsets.US_ASCII), new byte[] { 26, 0, 0, 0 },
			"WEBPVP8 ".getBytes(StandardCharsets.US_ASCII));

	public static final byte[] TEXT = "this is not an image".getBytes(StandardCharsets.UTF_8);

	private TestImages() {
	}

	private static byte[] concat(byte[]... parts) {
		byte[] result = new byte[0];
		for (byte[] part : parts) {
			int offset = result.length;
			result = Arrays.copyOf(result, offset + part.length);
			System.arraycopy(part, 0, result, offset, part.length);
		}
		return result;
	}

}
