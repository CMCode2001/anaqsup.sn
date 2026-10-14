-- =============================================================================
-- V2 — Contenu éditorial (workflow quatre yeux) et journal d'audit append-only
-- =============================================================================

CREATE TABLE news_category
(
    id       uuid PRIMARY KEY DEFAULT uuidv7(),
    slug     text    NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    label    text    NOT NULL,
    position integer NOT NULL DEFAULT 0
);

CREATE TABLE news_item
(
    id                  uuid PRIMARY KEY     DEFAULT uuidv7(),
    slug                text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    category_id         uuid        NOT NULL REFERENCES news_category (id),
    title               text        NOT NULL,
    summary             text,
    body_html           text        NOT NULL,
    cover_image_key     text,
    status              text        NOT NULL DEFAULT 'BROUILLON'
        CHECK (status IN ('BROUILLON', 'EN_RELECTURE', 'PUBLIE', 'ARCHIVE')),
    author              text        NOT NULL,
    reviewed_by         text,
    published_at        timestamptz,
    -- Traçabilité de la reprise : un article WordPress ne peut être importé qu'une fois.
    legacy_wordpress_id bigint UNIQUE,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    search_document     tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('french', immutable_unaccent(title)), 'A') ||
        setweight(to_tsvector('french', immutable_unaccent(coalesce(summary, ''))), 'B')
        ) STORED,

    -- Principe des quatre yeux, garanti par la base et pas seulement par l'interface.
    CONSTRAINT news_published_requires_review_chk CHECK (
        status <> 'PUBLIE' OR (published_at IS NOT NULL AND reviewed_by IS NOT NULL)),
    CONSTRAINT news_reviewer_is_not_author_chk CHECK (reviewed_by IS NULL OR reviewed_by <> author)
);

CREATE INDEX news_item_published_idx ON news_item (published_at DESC) WHERE status = 'PUBLIE';
CREATE INDEX news_item_search_idx ON news_item USING gin (search_document);

CREATE TABLE institutional_page
(
    id          uuid PRIMARY KEY     DEFAULT uuidv7(),
    slug        text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*(/[a-z0-9]+(-[a-z0-9]+)*)*$'),
    title       text        NOT NULL,
    body_html   text        NOT NULL,
    status      text        NOT NULL DEFAULT 'BROUILLON'
        CHECK (status IN ('BROUILLON', 'EN_RELECTURE', 'PUBLIE', 'ARCHIVE')),
    reviewed_at timestamptz,
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE audit_event
(
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    actor       text        NOT NULL,
    action      text        NOT NULL,
    target_type text,
    target_id   text,
    details     jsonb       NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX audit_event_target_idx ON audit_event (target_type, target_id, occurred_at DESC);

CREATE TRIGGER audit_event_append_only
    BEFORE UPDATE OR DELETE
    ON audit_event
    FOR EACH ROW
EXECUTE FUNCTION forbid_modification();
