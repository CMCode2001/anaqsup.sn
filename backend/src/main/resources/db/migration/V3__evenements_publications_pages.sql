-- =============================================================================
-- V3 — Site institutionnel : événements, publications, pages (workflow quatre yeux)
-- =============================================================================

CREATE TABLE event
(
    id              uuid PRIMARY KEY     DEFAULT uuidv7(),
    slug            text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    title           text        NOT NULL,
    summary         text,
    body_html       text        NOT NULL,
    location        text,
    starts_at       timestamptz NOT NULL,
    ends_at         timestamptz,
    cover_image_key text,
    status          text        NOT NULL DEFAULT 'BROUILLON'
        CHECK (status IN ('BROUILLON', 'EN_RELECTURE', 'PUBLIE', 'ARCHIVE')),
    author          text        NOT NULL,
    reviewed_by     text,
    published_at    timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT event_dates_chk CHECK (ends_at IS NULL OR ends_at >= starts_at),
    CONSTRAINT event_published_requires_review_chk CHECK (
        status <> 'PUBLIE' OR (published_at IS NOT NULL AND reviewed_by IS NOT NULL)),
    CONSTRAINT event_reviewer_is_not_author_chk CHECK (reviewed_by IS NULL OR reviewed_by <> author)
);

CREATE INDEX event_published_idx ON event (starts_at) WHERE status = 'PUBLIE';

CREATE TABLE publication
(
    id           uuid PRIMARY KEY     DEFAULT uuidv7(),
    slug         text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    title        text        NOT NULL,
    kind         text        NOT NULL CHECK (kind IN
                                             ('RAPPORT_ANNUEL', 'REFERENTIEL', 'GUIDE', 'TEXTE_REGLEMENTAIRE', 'DISCOURS',
                                              'ETUDE', 'AUTRE')),
    summary      text,
    published_on date        NOT NULL,
    file_url     text,
    status       text        NOT NULL DEFAULT 'BROUILLON'
        CHECK (status IN ('BROUILLON', 'EN_RELECTURE', 'PUBLIE', 'ARCHIVE')),
    author       text        NOT NULL,
    reviewed_by  text,
    published_at timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT publication_published_requires_review_chk CHECK (
        status <> 'PUBLIE' OR (published_at IS NOT NULL AND reviewed_by IS NOT NULL)),
    CONSTRAINT publication_reviewer_is_not_author_chk CHECK (reviewed_by IS NULL OR reviewed_by <> author)
);

CREATE INDEX publication_published_idx ON publication (kind, published_on DESC) WHERE status = 'PUBLIE';

-- Pages institutionnelles : même circuit de relecture que les actualités.
ALTER TABLE institutional_page
    ADD COLUMN summary      text,
    ADD COLUMN author       text NOT NULL DEFAULT 'reprise',
    ADD COLUMN reviewed_by  text,
    ADD COLUMN published_at timestamptz,
    ADD CONSTRAINT page_published_requires_review_chk CHECK (
        status <> 'PUBLIE' OR (published_at IS NOT NULL AND reviewed_by IS NOT NULL)),
    ADD CONSTRAINT page_reviewer_is_not_author_chk CHECK (reviewed_by IS NULL OR reviewed_by <> author);
