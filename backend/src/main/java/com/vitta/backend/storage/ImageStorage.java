package com.vitta.backend.storage;

/**
 * Where product images live. Implementations: {@link CloudinaryImageStorage} (when
 * {@code CLOUDINARY_URL} is set) and {@link LocalImageStorage} (otherwise).
 */
public interface ImageStorage {

	/**
	 * Stores an already validated image.
	 * @return its public URL and the key needed to delete it
	 */
	StoredImage store(byte[] content, ImageType type);

	/**
	 * Deletes a stored image; unknown keys are ignored.
	 */
	void delete(String key);

	record StoredImage(String url, String key) {
	}

}
