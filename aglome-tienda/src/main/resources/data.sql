-- Usuario admin (ya lo tenías)
INSERT INTO usuario (nombre, email, password, rol)
VALUES ('Administrador Aglome', 'admin@aglome.com', '$2b$10$FCMqmuP0liPH/zD0vEFIXuKhmXWIwJGxpnVNcaXP9IKQwWNzbg1F2', 'ADMIN');

-- Categoría de ejemplo
INSERT INTO categoria (nombre, activa) VALUES ('Melaminas', true);

-- Producto base de ejemplo
INSERT INTO producto_base (id, nombre, descripcion, marca, categoria_id, publicado)
VALUES ('11111111-1111-1111-1111-111111111111', 'Melamina Azul Marino', 'Aporta elegancia ejecutiva', 'Aglome', 1, true);

-- Tipos de atributo
INSERT INTO tipo_atributo (id, nombre) VALUES ('22222222-2222-2222-2222-222222222221', 'Color');
INSERT INTO tipo_atributo (id, nombre) VALUES ('22222222-2222-2222-2222-222222222222', 'Medida');

-- Variante de ejemplo
INSERT INTO variante (id, producto_base_id, sku, precio_venta, stock, activa)
VALUES ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'MELM-AZM-18MM', 45.90, 30, true);

-- Atributos de esa variante
INSERT INTO variante_atributo (variante_id, tipo_atributo_id, valor)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222221', 'Azul Marino');
INSERT INTO variante_atributo (variante_id, tipo_atributo_id, valor)
VALUES ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', '2.14m x 2.50m');