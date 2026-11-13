package com.vitta.backend.storage;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Image storage settings: {@code cloudinaryUrl} from {@code CLOUDINARY_URL} (if set, Cloudinary
 * is used) and {@code uploadsDir} from {@code UPLOADS_DIR} (local storage otherwise).
 */
@ConfigurationProperties("app.storage")
public record StorageProperties(String cloudinaryUrl, @DefaultValue("./uploads") Path uploadsDir) {
}
