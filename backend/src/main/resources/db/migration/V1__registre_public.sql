-- =============================================================================
-- V1 — Registre public : le portail est la source de vérité (ADR-0004)
-- Discipline : forward-only, expand/contract (voir docs/runbooks/rollback.md)
-- =============================================================================

-- Extensions « trusted » (PG13+) : créables par le propriétaire de la base, sans superutilisateur.
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() n'est pas IMMUTABLE : ce wrapper l'est, pour les colonnes générées et index.
CREATE FUNCTION immutable_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN public.unaccent('public.unaccent'::regdictionary, $1);

CREATE FUNCTION forbid_modification() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'Table % en ajout seul : % interdit', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END
$$;

-- -----------------------------------------------------------------------------
-- Établissements
-- -----------------------------------------------------------------------------
CREATE TABLE establishment
(
    id              uuid PRIMARY KEY     DEFAULT uuidv7(),
    slug            text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    name            text        NOT NULL,
    acronym         text,
    kind            text        NOT NULL CHECK (kind IN ('PUBLIC', 'PRIVE')),
    city            text,
    region          text,
    website         text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    search_document tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('french', immutable_unaccent(coalesce(acronym, ''))), 'A') ||
        setweight(to_tsvector('french', immutable_unaccent(name)), 'A') ||
        setweight(to_tsvector('french', immutable_unaccent(coalesce(city, ''))), 'C')
        ) STORED
);

CREATE INDEX establishment_search_idx ON establishment USING gin (search_document);
CREATE INDEX establishment_name_trgm_idx ON establishment USING gin (immutable_unaccent(lower(name)) gin_trgm_ops);

-- Sigles, anciens noms, anciens slugs WordPress : on cherche « ucad », pas « universite-cheikh-anta-diop ».
CREATE TABLE establishment_alias
(
    establishment_id uuid NOT NULL REFERENCES establishment (id) ON DELETE CASCADE,
    alias            text NOT NULL,
    kind             text NOT NULL CHECK (kind IN ('SIGLE', 'ANCIEN_NOM', 'ANCIEN_SLUG')),
    PRIMARY KEY (establishment_id, alias)
);

CREATE INDEX establishment_alias_lookup_idx ON establishment_alias (immutable_unaccent(lower(alias)));

-- -----------------------------------------------------------------------------
-- Formations
-- -----------------------------------------------------------------------------
CREATE TABLE program
(
    id               uuid PRIMARY KEY     DEFAULT uuidv7(),
    slug             text        NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    establishment_id uuid        NOT NULL REFERENCES establishment (id),
    title            text        NOT NULL,
    level            text        NOT NULL CHECK (level IN ('LICENCE', 'MASTER', 'DOCTORAT', 'DUT', 'BTS', 'AUTRE')),
    domain           text,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    search_document  tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('french', immutable_unaccent(title)), 'A') ||
        setweight(to_tsvector('french', immutable_unaccent(coalesce(domain, ''))), 'B')
        ) STORED
);

CREATE INDEX program_establishment_idx ON program (establishment_id);
CREATE INDEX program_search_idx ON program USING gin (search_document);
CREATE INDEX program_title_trgm_idx ON program USING gin (immutable_unaccent(lower(title)) gin_trgm_ops);

-- -----------------------------------------------------------------------------
-- Décisions : saisies dans le back-office, publiées par une seconde personne,
-- puis immuables (faits juridiques)
-- -----------------------------------------------------------------------------
CREATE TABLE decision
(
    id               uuid PRIMARY KEY     DEFAULT uuidv7(),
    reference        text        NOT NULL UNIQUE CHECK (reference ~ '^[A-Z0-9-]{3,40}$'),
    type             text        NOT NULL CHECK (type IN ('HABILITATION', 'ACCREDITATION')),
    -- Deux clés étrangères exclusives plutôt qu'un subject_id polymorphe : l'intégrité est garantie.
    establishment_id uuid REFERENCES establishment (id),
    program_id       uuid REFERENCES program (id),
    outcome          text        NOT NULL CHECK (outcome IN
                                                 ('FAVORABLE', 'FAVORABLE_AVEC_RESERVES', 'DEFAVORABLE', 'SUSPENSION',
                                                  'RETRAIT')),
    decision_date    date        NOT NULL,
    effective_from   date        NOT NULL,
    valid_until      date,
    published_at     timestamptz,
    rectifies        text REFERENCES decision (reference),
    origin           text        NOT NULL DEFAULT 'SAISIE_BACKOFFICE' CHECK (origin IN ('SAISIE_BACKOFFICE', 'REPRISE_WORDPRESS')),
    entered_by       text        NOT NULL,
    published_by     text,
    created_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT decision_validity_chk CHECK (valid_until IS NULL OR valid_until >= effective_from),
    CONSTRAINT decision_subject_chk CHECK (
        (type = 'HABILITATION' AND establishment_id IS NOT NULL AND program_id IS NULL) OR
        (type = 'ACCREDITATION' AND program_id IS NOT NULL AND establishment_id IS NULL)),
    CONSTRAINT decision_not_self_rectifying_chk CHECK (rectifies IS NULL OR rectifies <> reference),
    -- Principe des quatre yeux : la personne qui publie n'est pas celle qui a saisi.
    CONSTRAINT decision_four_eyes_chk CHECK (
        published_at IS NULL OR (published_by IS NOT NULL AND published_by <> entered_by))
);

CREATE INDEX decision_establishment_idx ON decision (establishment_id, effective_from DESC) WHERE establishment_id IS NOT NULL;
CREATE INDEX decision_program_idx ON decision (program_id, effective_from DESC) WHERE program_id IS NOT NULL;
-- Flux de modifications pour les partenaires (updatedSince + cursor).
CREATE INDEX decision_feed_idx ON decision (published_at, id) WHERE published_at IS NOT NULL;

-- Publier (published_at NULL → valeur) est permis ; modifier ou supprimer une décision publiée ne l'est pas.
CREATE FUNCTION forbid_published_decision_change() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.published_at IS NOT NULL THEN
        RAISE EXCEPTION 'Décision % publiée : immuable. Publier un rectificatif (colonne rectifies).', OLD.reference
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER decision_immutable_once_published
    BEFORE UPDATE OR DELETE
    ON decision
    FOR EACH ROW
EXECUTE FUNCTION forbid_published_decision_change();

-- -----------------------------------------------------------------------------
-- Documents officiels vérifiables (QR code + code + SHA-256)
-- -----------------------------------------------------------------------------
CREATE TABLE official_document
(
    id                uuid PRIMARY KEY     DEFAULT uuidv7(),
    verification_code text        NOT NULL UNIQUE CHECK (verification_code ~ '^[A-Z0-9]{4}(-[A-Z0-9]{4}){3}$'),
    kind              text        NOT NULL CHECK (kind IN
                                                  ('DECISION', 'RAPPORT_EVALUATION', 'REFERENTIEL', 'TEXTE_REGLEMENTAIRE',
                                                   'AUTRE')),
    title             text        NOT NULL,
    decision_id       uuid REFERENCES decision (id),
    sha256            char(64)    NOT NULL CHECK (sha256 ~ '^[a-f0-9]{64}$'),
    media_type        text        NOT NULL DEFAULT 'application/pdf',
    size_bytes        bigint      NOT NULL CHECK (size_bytes > 0),
    storage_key       text        NOT NULL UNIQUE,
    published_at      timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX official_document_decision_idx ON official_document (decision_id);
