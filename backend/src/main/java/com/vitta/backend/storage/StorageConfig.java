package com.vitta.backend.storage;

import com.cloudinary.Cloudinary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Chooses the image storage: Cloudinary when {@code CLOUDINARY_URL} is set, local disk otherwise.
 * In local mode the files are also served at {@code /uploads/**}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

	private final StorageProperties properties;

	public StorageConfig(StorageProperties properties) {
		this.properties = properties;
	}

	@Bean
	ImageStorage imageStorage() {
		if (usesCloudinary()) {
			log.info("Image storage: Cloudinary (folder {})", CloudinaryImageStorage.FOLDER);
			return new CloudinaryImageStorage(new Cloudinary(properties.cloudinaryUrl()));
		}
		LocalImageStorage storage = new LocalImageStorage(properties.uploadsDir());
		log.info("Image storage: local directory {}", storage.root());
		return storage;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		if (!usesCloudinary()) {
			String location = properties.uploadsDir().toAbsolutePath().normalize().toUri().toString();
			registry.addResourceHandler(LocalImageStorage.URL_PREFIX + "**").addResourceLocations(location);
		}
	}

	private boolean usesCloudinary() {
		return StringUtils.hasText(properties.cloudinaryUrl());
	}

}
