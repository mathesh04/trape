-- Update admin credentials to admin@trape / 2004@admin
-- BCrypt hash generated for '2004@admin': $2a$10$LGxpLGLurnq/S4gC.W4Uj.rvXYkb4TPtRbhgklwHONkKgswMrJ122

insert into users (id, email, password_hash, full_name, role)
values ('00000000-0000-0000-0000-000000000001', 'admin@trape', '$2a$10$LGxpLGLurnq/S4gC.W4Uj.rvXYkb4TPtRbhgklwHONkKgswMrJ122', 'Trape Admin', 'SUPER_ADMIN')
on conflict (id) do update
set email = 'admin@trape',
    password_hash = '$2a$10$LGxpLGLurnq/S4gC.W4Uj.rvXYkb4TPtRbhgklwHONkKgswMrJ122';
