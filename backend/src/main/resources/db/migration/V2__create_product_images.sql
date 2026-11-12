-- Ordered image gallery per product. products.image_url stays as the cover used by listings.
CREATE TABLE product_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    product_id BIGINT       NOT NULL,
    url        VARCHAR(500) NOT NULL,
    position   INT          NOT NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT uk_product_images_product_position UNIQUE (product_id, position),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_product_images_position CHECK (position >= 0)
);

-- Backfill: the existing cover image becomes the first image of the gallery.
INSERT INTO product_images (product_id, url, position)
SELECT id, image_url, 0
FROM products
WHERE image_url IS NOT NULL;
