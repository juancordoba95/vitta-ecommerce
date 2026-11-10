CREATE TABLE categories (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE products (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)   NOT NULL,
    slug        VARCHAR(170)   NOT NULL,
    description VARCHAR(2000),
    price       DECIMAL(10, 2) NOT NULL,
    category_id BIGINT         NOT NULL,
    image_url   VARCHAR(500),
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_price CHECK (price >= 0)
);

CREATE TABLE product_variants (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    product_id BIGINT      NOT NULL,
    size       VARCHAR(10) NOT NULL,
    stock      INT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT uk_product_variants_product_size UNIQUE (product_id, size),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_product_variants_stock CHECK (stock >= 0)
);
