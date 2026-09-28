-- Remove the demo/seed products that shipped with V2__seed_data.sql
-- (Oxford Cotton Shirt, Oversized Drop-Shoulder Tee, Selvedge Straight Denim,
--  Unstructured Linen Blazer) so the catalog starts empty and ready for the
-- admin to add real products with real photos.
--
-- Categories, coupons, and users created by the seed migration are left in
-- place — only the placeholder products (and anything that hangs off them)
-- are removed.

-- Drop any cart/order line items that reference the seeded variants first,
-- since those two tables do not cascade on variant deletion.
delete from cart_items
where variant_id in (
  select id from product_variants
  where product_id in (
    '20000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000004'
  )
);

delete from order_items
where variant_id in (
  select id from product_variants
  where product_id in (
    '20000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000004'
  )
);

-- Deleting the products cascades to product_images, product_variants,
-- inventory (via variants), reviews, and wishlist_items automatically.
delete from products
where id in (
  '20000000-0000-0000-0000-000000000001',
  '20000000-0000-0000-0000-000000000002',
  '20000000-0000-0000-0000-000000000003',
  '20000000-0000-0000-0000-000000000004'
);
