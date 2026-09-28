-- Seed data: admin user, categories, sample products/variants/inventory
-- Default admin login: admin@trape.in / Admin@123   (CHANGE THIS after first login)
-- Default test customer: customer@trape.in / Test@123

insert into users (id, email, password_hash, full_name, role) values
  ('00000000-0000-0000-0000-000000000001', 'admin@trape.in',
   '$2b$12$rlOISuLcZkkUjILj/9MxNOSGSPMiuzpvN9Bdxqw0uiLxOhl.Qx7a6',
   'Trape Admin', 'SUPER_ADMIN'),
  ('00000000-0000-0000-0000-000000000002', 'customer@trape.in',
   '$2b$12$C9b1lKcSqICyHLLtjAw4sOqmJ.GpshFRd.uhbpKMeGyEQSOkhMPhu',
   'Test Customer', 'CUSTOMER');

insert into categories (id, parent_id, name, slug, display_order) values
  ('10000000-0000-0000-0000-000000000001', null, 'Shirts', 'shirts', 1),
  ('10000000-0000-0000-0000-000000000002', null, 'T-Shirts', 't-shirts', 2),
  ('10000000-0000-0000-0000-000000000003', null, 'Denim', 'denim', 3),
  ('10000000-0000-0000-0000-000000000004', null, 'Blazers', 'blazers', 4);

insert into products (id, category_id, name, slug, description, brand, base_price, status) values
  ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
   'Oxford Cotton Shirt', 'oxford-cotton-shirt',
   'A tailored, premium oxford cotton shirt with a modern slim fit — breathable, wrinkle-resistant, built for all-day wear.',
   'Trape', 1999.00, 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002',
   'Oversized Drop-Shoulder Tee', 'oversized-drop-shoulder-tee',
   'Heavyweight 240 GSM cotton tee with a relaxed drop-shoulder silhouette.',
   'Trape', 1299.00, 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003',
   'Selvedge Straight Denim', 'selvedge-straight-denim',
   'Japanese selvedge denim, straight fit, raw indigo wash that develops character over time.',
   'Trape', 3499.00, 'ACTIVE'),
  ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004',
   'Unstructured Linen Blazer', 'unstructured-linen-blazer',
   'Lightweight unstructured blazer in a linen-cotton blend, perfect for warm-weather formal wear.',
   'Trape', 5999.00, 'ACTIVE');

insert into product_images (product_id, url, alt_text, display_order) values
  ('20000000-0000-0000-0000-000000000001', '/images/products/oxford-shirt-1.jpg', 'Oxford Cotton Shirt front', 1),
  ('20000000-0000-0000-0000-000000000001', '/images/products/oxford-shirt-2.jpg', 'Oxford Cotton Shirt back', 2),
  ('20000000-0000-0000-0000-000000000002', '/images/products/oversized-tee-1.jpg', 'Oversized Tee front', 1),
  ('20000000-0000-0000-0000-000000000003', '/images/products/denim-1.jpg', 'Selvedge Denim front', 1),
  ('20000000-0000-0000-0000-000000000004', '/images/products/blazer-1.jpg', 'Linen Blazer front', 1);

insert into product_variants (id, product_id, sku, size, color, price, mrp, is_active) values
  ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'OXF-SHT-WHT-S', 'S', 'White', 1999.00, 2499.00, true),
  ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'OXF-SHT-WHT-M', 'M', 'White', 1999.00, 2499.00, true),
  ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001', 'OXF-SHT-WHT-L', 'L', 'White', 1999.00, 2499.00, true),
  ('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 'OVR-TEE-BLK-M', 'M', 'Black', 1299.00, 1599.00, true),
  ('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000002', 'OVR-TEE-BLK-L', 'L', 'Black', 1299.00, 1599.00, true),
  ('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000003', 'DNM-SEL-IND-32', '32', 'Indigo', 3499.00, 3999.00, true),
  ('30000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000003', 'DNM-SEL-IND-34', '34', 'Indigo', 3499.00, 3999.00, true),
  ('30000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000004', 'BLZ-LIN-BEG-M', 'M', 'Beige', 5999.00, 6999.00, true);

insert into inventory (variant_id, warehouse_id, quantity_available, quantity_reserved) values
  ('30000000-0000-0000-0000-000000000001', 'DEFAULT', 25, 0),
  ('30000000-0000-0000-0000-000000000002', 'DEFAULT', 40, 0),
  ('30000000-0000-0000-0000-000000000003', 'DEFAULT', 30, 0),
  ('30000000-0000-0000-0000-000000000004', 'DEFAULT', 50, 0),
  ('30000000-0000-0000-0000-000000000005', 'DEFAULT', 35, 0),
  ('30000000-0000-0000-0000-000000000006', 'DEFAULT', 15, 0),
  ('30000000-0000-0000-0000-000000000007', 'DEFAULT', 12, 0),
  ('30000000-0000-0000-0000-000000000008', 'DEFAULT', 10, 0);

insert into coupons (code, discount_type, discount_value, min_cart_value, valid_from, valid_until, usage_limit, is_active) values
  ('WELCOME10', 'PERCENT', 10.00, 999.00, now(), now() + interval '1 year', 1000, true),
  ('FLAT500', 'FLAT', 500.00, 2999.00, now(), now() + interval '1 year', 500, true);
