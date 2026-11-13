-- Identifier of the file in the image storage (Cloudinary public_id or local path), used to
-- delete it. NULL for external images (e.g. dev seed URLs), which are only removed from the DB.
ALTER TABLE product_images ADD COLUMN storage_key VARCHAR(300) NULL;
