-- Extensión de PostgreSQL que quita las tildes de un texto. La usa la búsqueda de profesionales por
-- nombre, que no distingue tildes ni mayúsculas (HU-05). La regla está en el puerto de salida del
-- dominio; esta es la forma en que la cumple el adaptador de PostgreSQL.
--
-- Es una extensión "trusted": la puede crear el dueño de la base, sin ser superusuario.
CREATE EXTENSION IF NOT EXISTS unaccent;
