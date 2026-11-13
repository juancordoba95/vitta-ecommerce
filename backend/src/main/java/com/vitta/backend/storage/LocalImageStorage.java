package com.vitta.backend.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Stores images on the local disk under {@code uploadsDir}; they are served by the app itself at
 * {@code /uploads/**}. Meant for development and simple setups without Cloudinary.
 */
public class LocalImageStorage implements ImageStorage {

	public static final String URL_PREFIX = "/uploads/";

	private static final String FOLDER = "products";

	private final Path root;

	public LocalImageStorage(Path uploadsDir) {
		this.root = uploadsDir.toAbsolutePath().normalize();
	}

	public Path root() {
		return root;
	}

	@Override
	public StoredImage store(byte[] content, ImageType type) {
		String key = FOLDER + "/" + UUID.randomUUID() + "." + type.extension();
		Path target = resolve(key);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		}
		catch (IOException ex) {
			throw new ImageStorageException("Could not store image locally", ex);
		}
		return new StoredImage(publicUrl(key), key);
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		}
		catch (IOException ex) {
			throw new ImageStorageException("Could not delete local image " + key, ex);
		}
	}

	private Path resolve(String key) {
		Path path = root.resolve(key).normalize();
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Storage key outside the uploads directory: " + key);
		}
		return path;
	}

	/** Absolute URL when called within a request (the frontend runs on another origin). */
	private static String publicUrl(String key) {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return URL_PREFIX + key;
		}
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(URL_PREFIX).path(key).toUriString();
	}

}
