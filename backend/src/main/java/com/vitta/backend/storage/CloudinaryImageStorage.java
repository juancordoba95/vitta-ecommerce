package com.vitta.backend.storage;

import java.io.IOException;
import java.util.Map;

import com.cloudinary.Cloudinary;

/**
 * Stores images in Cloudinary (folder {@value #FOLDER}). The storage key is the Cloudinary
 * {@code public_id}.
 */
public class CloudinaryImageStorage implements ImageStorage {

	public static final String FOLDER = "vitta/products";

	private final Cloudinary cloudinary;

	public CloudinaryImageStorage(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	@Override
	public StoredImage store(byte[] content, ImageType type) {
		try {
			Map<?, ?> result = cloudinary.uploader()
				.upload(content, Map.of("folder", FOLDER, "resource_type", "image"));
			return new StoredImage((String) result.get("secure_url"), (String) result.get("public_id"));
		}
		catch (IOException | RuntimeException ex) {
			throw new ImageStorageException("Could not upload image to Cloudinary", ex);
		}
	}

	@Override
	public void delete(String key) {
		try {
			cloudinary.uploader().destroy(key, Map.of("resource_type", "image"));
		}
		catch (IOException ex) {
			throw new ImageStorageException("Could not delete image " + key + " from Cloudinary", ex);
		}
	}

}
