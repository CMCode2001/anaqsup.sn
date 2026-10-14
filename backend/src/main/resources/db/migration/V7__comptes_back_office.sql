-- Comptes du back-office (décisions ANAQ-Sup du 13/10/2026, ADR-0006).
-- - Rôles : « Administrateur » (tous les droits, non modifiable) et « Chargé de communication » ;
--   l'administrateur peut créer d'autres rôles en choisissant leurs permissions.
-- - Comptes créés par l'administrateur avec un mot de passe provisoire, à changer à la première connexion.
-- - Mots de passe : 8 caractères minimum (contrôlé par l'API), stockés hachés (bcrypt), jamais en clair.

CREATE TABLE admin_role
(
    id          uuid PRIMARY KEY     DEFAULT uuidv7(),
    name        text        NOT NULL CHECK (length(btrim(name)) BETWEEN 2 AND 60),
    -- COMPTES : gérer comptes et rôles ; les autres : publier dans la rubrique correspondante.
    permissions text[]      NOT NULL CHECK (
        permissions <@ ARRAY ['COMPTES', 'ACTUALITES', 'EVENEMENTS', 'DOCUMENTS', 'PAGES', 'AVIS']::text[]),
    system      boolean     NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX admin_role_name_idx ON admin_role (lower(btrim(name)));
-- Un seul rôle système : l'Administrateur.
CREATE UNIQUE INDEX admin_role_single_system_idx ON admin_role ((true)) WHERE system;

CREATE TABLE admin_account
(
    id                   uuid PRIMARY KEY     DEFAULT uuidv7(),
    email                text        NOT NULL CHECK (email ~ '^[^@\s]+@[^@\s]+$'),
    display_name         text        NOT NULL CHECK (length(btrim(display_name)) BETWEEN 1 AND 120),
    password_hash        text        NOT NULL,
    role_id              uuid        NOT NULL REFERENCES admin_role (id),
    must_change_password boolean     NOT NULL DEFAULT true,
    enabled              boolean     NOT NULL DEFAULT true,
    last_login_at        timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX admin_account_email_idx ON admin_account (lower(email));
CREATE INDEX admin_account_role_idx ON admin_account (role_id);

INSERT INTO admin_role (name, permissions, system)
VALUES ('Administrateur', ARRAY ['COMPTES', 'ACTUALITES', 'EVENEMENTS', 'DOCUMENTS', 'PAGES', 'AVIS'], true),
       ('Chargé de communication', ARRAY ['ACTUALITES', 'EVENEMENTS', 'DOCUMENTS', 'PAGES'], false);
