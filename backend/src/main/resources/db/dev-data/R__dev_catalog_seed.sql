-- Sample catalog data for local development only.
-- Loaded through the "dev" profile (see application-dev.yaml); never runs in other environments.
-- Repeatable migration: re-applied when this file changes, so every statement is idempotent.

INSERT INTO categories (name, slug)
VALUES ('Conjuntos', 'conjuntos'),
       ('Bodies', 'bodies'),
       ('Accesorios', 'accesorios') AS new
ON DUPLICATE KEY UPDATE name = new.name;

INSERT INTO products (name, slug, description, price, category_id, image_url, active)
VALUES ('Conjunto Encaje Floral', 'conjunto-encaje-floral',
        'Conjunto de corpiño con aro y bombacha tiro medio en encaje floral negro. Breteles regulables y broche trasero de tres posiciones.',
        42900.00, (SELECT id FROM categories WHERE slug = 'conjuntos'),
        'https://placehold.co/600x800?text=Conjunto+Encaje+Floral', TRUE),
       ('Conjunto Tul Bordado', 'conjunto-tul-bordado',
        'Corpiño triángulo sin aro y colaless en tul color nude con bordado de flores. Liviano y cómodo para el día a día.',
        39900.00, (SELECT id FROM categories WHERE slug = 'conjuntos'),
        'https://placehold.co/600x800?text=Conjunto+Tul+Bordado', TRUE),
       ('Conjunto Satén Rosa', 'conjunto-saten-rosa',
        'Conjunto de satén rosa empolvado con detalles de puntilla. Corpiño con taza soft y bombacha culotte.',
        36500.00, (SELECT id FROM categories WHERE slug = 'conjuntos'),
        'https://placehold.co/600x800?text=Conjunto+Saten+Rosa', TRUE),
       ('Body Encaje Negro', 'body-encaje-negro',
        'Body de encaje negro con escote en V profundo, espalda descubierta y broches en la entrepierna.',
        45900.00, (SELECT id FROM categories WHERE slug = 'bodies'),
        'https://placehold.co/600x800?text=Body+Encaje+Negro', TRUE),
       ('Body Tul Bordo', 'body-tul-bordo',
        'Body de tul transparente color bordo con aplicaciones de encaje en el busto y tiras cruzadas en la espalda.',
        41500.00, (SELECT id FROM categories WHERE slug = 'bodies'),
        'https://placehold.co/600x800?text=Body+Tul+Bordo', TRUE),
       ('Body Lycra Básico', 'body-lycra-basico',
        'Body de lycra negra con escote cuadrado y breteles finos. Ideal para usar debajo de un blazer o con jean.',
        28900.00, (SELECT id FROM categories WHERE slug = 'bodies'),
        'https://placehold.co/600x800?text=Body+Lycra+Basico', FALSE),
       ('Portaligas de Encaje', 'portaligas-de-encaje',
        'Portaligas de encaje negro con cuatro tiras regulables y broches metálicos. Combina con los conjuntos de encaje.',
        18900.00, (SELECT id FROM categories WHERE slug = 'accesorios'),
        'https://placehold.co/600x800?text=Portaligas+de+Encaje', TRUE),
       ('Bata de Satén', 'bata-de-saten',
        'Bata kimono de satén champagne con mangas tres cuartos, cinto al tono y terminación de puntilla.',
        52000.00, (SELECT id FROM categories WHERE slug = 'accesorios'),
        'https://placehold.co/600x800?text=Bata+de+Saten', TRUE) AS new
ON DUPLICATE KEY UPDATE name        = new.name,
                        description = new.description,
                        price       = new.price,
                        category_id = new.category_id,
                        image_url   = new.image_url,
                        active      = new.active;

INSERT INTO product_variants (product_id, size, stock)
VALUES ((SELECT id FROM products WHERE slug = 'conjunto-encaje-floral'), 'S', 4),
       ((SELECT id FROM products WHERE slug = 'conjunto-encaje-floral'), 'M', 7),
       ((SELECT id FROM products WHERE slug = 'conjunto-encaje-floral'), 'L', 3),
       ((SELECT id FROM products WHERE slug = 'conjunto-tul-bordado'), 'S', 6),
       ((SELECT id FROM products WHERE slug = 'conjunto-tul-bordado'), 'M', 0),
       ((SELECT id FROM products WHERE slug = 'conjunto-tul-bordado'), 'L', 2),
       ((SELECT id FROM products WHERE slug = 'conjunto-saten-rosa'), 'S', 5),
       ((SELECT id FROM products WHERE slug = 'conjunto-saten-rosa'), 'M', 5),
       ((SELECT id FROM products WHERE slug = 'conjunto-saten-rosa'), 'L', 4),
       ((SELECT id FROM products WHERE slug = 'body-encaje-negro'), 'S', 3),
       ((SELECT id FROM products WHERE slug = 'body-encaje-negro'), 'M', 6),
       ((SELECT id FROM products WHERE slug = 'body-encaje-negro'), 'L', 0),
       ((SELECT id FROM products WHERE slug = 'body-tul-bordo'), 'S', 2),
       ((SELECT id FROM products WHERE slug = 'body-tul-bordo'), 'M', 4),
       ((SELECT id FROM products WHERE slug = 'body-tul-bordo'), 'L', 3),
       ((SELECT id FROM products WHERE slug = 'body-lycra-basico'), 'S', 8),
       ((SELECT id FROM products WHERE slug = 'body-lycra-basico'), 'M', 10),
       ((SELECT id FROM products WHERE slug = 'body-lycra-basico'), 'L', 6),
       ((SELECT id FROM products WHERE slug = 'portaligas-de-encaje'), 'S', 5),
       ((SELECT id FROM products WHERE slug = 'portaligas-de-encaje'), 'M', 5),
       ((SELECT id FROM products WHERE slug = 'portaligas-de-encaje'), 'L', 1),
       ((SELECT id FROM products WHERE slug = 'bata-de-saten'), 'S', 2),
       ((SELECT id FROM products WHERE slug = 'bata-de-saten'), 'M', 3),
       ((SELECT id FROM products WHERE slug = 'bata-de-saten'), 'L', 2) AS new
ON DUPLICATE KEY UPDATE stock = new.stock;
