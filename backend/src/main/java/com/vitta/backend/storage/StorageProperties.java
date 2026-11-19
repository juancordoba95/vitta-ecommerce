package com.vitta.backend.storage;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * Image storage settings: {@code cloudinaryUrl} from {@code CLOUDINARY_URL} (if set, Cloudinary
 * is used) and {@code uploadsDir} from {@code UPLOADS_DIR} (local storage otherwise).
 * {@code requireCloudinary} (true in the prod profile) makes a missing {@code CLOUDINARY_URL}
 * fail startup, because local files would be lost on hosts without a persistent disk.
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(String cloudinaryUrl, @DefaultValue("./uploads") Path uploadsDir,
		@DefaultValue("false") boolean requireCloudinary) {

	public StorageProperties {
		if (requireCloudinary && !StringUtils.hasText(cloudinaryUrl)) {
			throw new IllegalArgumentException("app.storage.cloudinary-url (CLOUDINARY_URL) is required when "
					+ "app.storage.require-cloudinary is true (prod profile): the server disk is not persistent");
		}
	}

}
