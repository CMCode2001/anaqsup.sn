-- Accueil v2 (décisions ANAQ-Sup du 13/10/2026) :
-- - images téléversées depuis le back-office (photo du Secrétaire exécutif, logos des partenaires…) ;
-- - photo et légende (nom, fonction) des pages institutionnelles ;
-- - partenaires gérés dans le back-office, affichés en bandeau sur l'accueil.

CREATE TABLE media
(
    id            uuid PRIMARY KEY     DEFAULT uuidv7(),
    -- Formats d'image acceptés, vérifiés sur le contenu du fichier (pas seulement l'extension).
    -- Pas de SVG : un SVG peut contenir du script.
    content_type  text        NOT NULL CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp')),
    size_bytes    integer     NOT NULL CHECK (size_bytes > 0),
    sha256        text        NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    original_name text,
    uploaded_by   text        NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE institutional_page
    ADD COLUMN image_key        text,
    ADD COLUMN image_caption    text,
    ADD COLUMN image_caption_en text;

CREATE TABLE partner
(
    id         uuid PRIMARY KEY     DEFAULT uuidv7(),
    name       text        NOT NULL CHECK (length(btrim(name)) BETWEEN 1 AND 120),
    url        text CHECK (url IS NULL OR url ~ '^https?://'),
    logo_key   text,
    position   integer     NOT NULL DEFAULT 0,
    enabled    boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX partner_position_idx ON partner (position, name) WHERE enabled;

-- Partenaires choisis par l'ANAQ-Sup ; logos à fournir, puis à ajouter dans le back-office.
INSERT INTO partner (name, url, position)
VALUES ('MESRI', 'https://mesrisenegal.sn', 1),
       ('CAMES', 'https://www.lecames.org', 2),
       ('CAMPUSEN', 'https://www.campusen.sn', 3),
       ('UNESCO', 'https://www.unesco.org/fr/fieldoffice/dakar', 4);

-- Nouvelle permission « PARTENAIRES », accordée à l'Administrateur.
ALTER TABLE admin_role DROP CONSTRAINT admin_role_permissions_check;
ALTER TABLE admin_role ADD CONSTRAINT admin_role_permissions_check CHECK (
    permissions <@ ARRAY ['COMPTES', 'ACTUALITES', 'EVENEMENTS', 'DOCUMENTS', 'PAGES', 'AVIS', 'PARTENAIRES']::text[]);
UPDATE admin_role SET permissions = array_append(permissions, 'PARTENAIRES')
WHERE system AND NOT ('PARTENAIRES' = ANY (permissions));
