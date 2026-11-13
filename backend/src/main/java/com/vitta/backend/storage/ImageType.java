package com.vitta.backend.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Accepted image formats, detected from the file's leading bytes ("magic numbers") rather than
 * from its name or the client-declared content type.
 */
public enum ImageType {

	JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), WEBP("webp", "image/webp");

	private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

	private static final byte[] PNG_MAGIC = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

	private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);

	private static final byte[] WEBP_TAG = "WEBP".getBytes(StandardCharsets.US_ASCII);

	private final String extension;

	private final String mediaType;

	ImageType(String extension, String mediaType) {
		this.extension = extension;
		this.mediaType = mediaType;
	}

	public String extension() {
		return extension;
	}

	public String mediaType() {
		return mediaType;
	}

	public static Optional<ImageType> detect(byte[] content) {
		if (startsWith(content, 0, JPEG_MAGIC)) {
			return Optional.of(JPEG);
		}
		if (startsWith(content, 0, PNG_MAGIC)) {
			return Optional.of(PNG);
		}
		// RIFF container: "RIFF" + 4-byte size + "WEBP".
		if (startsWith(content, 0, RIFF) && startsWith(content, 8, WEBP_TAG)) {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	private static boolean startsWith(byte[] content, int offset, byte[] prefix) {
		return content != null && content.length >= offset + prefix.length
				&& Arrays.equals(content, offset, offset + prefix.length, prefix, 0, prefix.length);
	}

}
