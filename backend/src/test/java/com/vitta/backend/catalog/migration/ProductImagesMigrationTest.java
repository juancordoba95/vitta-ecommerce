package com.vitta.backend.catalog.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the data backfill of {@code V2__create_product_images}: products that existed before
 * V2 get their cover image copied to {@code product_images} at position 0. Runs only where Docker
 * is available (CI).
 */
class ProductImagesMigrationTest {

	private static final MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@BeforeAll
	static void startDatabase() {
		mysql.start();
	}

	@AfterAll
	static void stopDatabase() {
		mysql.stop();
	}

	@Test
	void copiesExistingCoverImagesToTheGallery() throws SQLException {
		migrateTo("1");
		try (Connection connection = connect(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO categories (name, slug) VALUES ('Conjuntos', 'conjuntos')");
			statement.executeUpdate("""
					INSERT INTO products (name, slug, price, category_id, image_url, active)
					SELECT 'Con portada', 'con-portada', 100.00, id, 'https://img/cover.jpg', TRUE FROM categories
					UNION ALL
					SELECT 'Inactivo', 'inactivo', 100.00, id, 'https://img/inactive.jpg', FALSE FROM categories
					UNION ALL
					SELECT 'Sin portada', 'sin-portada', 100.00, id, NULL, TRUE FROM categories
					""");
		}

		migrateTo("2");

		assertThat(galleryRows()).containsExactly("con-portada|https://img/cover.jpg|0",
				"inactivo|https://img/inactive.jpg|0");
	}

	private static void migrateTo(String version) {
		Flyway.configure()
			.dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
			.locations("classpath:db/migration")
			.target(version)
			.load()
			.migrate();
	}

	private static List<String> galleryRows() throws SQLException {
		List<String> rows = new ArrayList<>();
		try (Connection connection = connect();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("""
						SELECT p.slug, i.url, i.position
						FROM product_images i JOIN products p ON p.id = i.product_id
						ORDER BY p.slug, i.position
						""")) {
			while (resultSet.next()) {
				rows.add(resultSet.getString(1) + "|" + resultSet.getString(2) + "|" + resultSet.getInt(3));
			}
		}
		return rows;
	}

	private static Connection connect() throws SQLException {
		return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
	}

}
