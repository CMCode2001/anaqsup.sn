-- =============================================================================
-- V4 — Arbitrages de la maîtrise d'ouvrage (13/10/2026) :
--   * habilitations et accréditations en LISTES STRUCTURÉES : avis (favorable / défavorable)
--     et année saisis tels quels sur la fiche — plus de décisions ni de statut calculé ;
--   * PUBLICATION DIRECTE des contenus : plus de relecture obligatoire par une seconde personne.
-- =============================================================================

DROP TABLE official_document;
DROP TABLE decision;
DROP FUNCTION forbid_published_decision_change();

-- -----------------------------------------------------------------------------
-- Avis d'habilitation (établissement) et d'accréditation (formation)
-- -----------------------------------------------------------------------------
ALTER TABLE establishment
    ADD COLUMN habilitation_avis      text CHECK (habilitation_avis IN ('FAVORABLE', 'DEFAVORABLE')),
    ADD COLUMN habilitation_avis_year integer CHECK (habilitation_avis_year BETWEEN 2000 AND 2100),
    ADD CONSTRAINT establishment_avis_year_chk CHECK ((habilitation_avis IS NULL) = (habilitation_avis_year IS NULL));

ALTER TABLE program
    ADD COLUMN accreditation_avis      text CHECK (accreditation_avis IN ('FAVORABLE', 'DEFAVORABLE')),
    ADD COLUMN accreditation_avis_year integer CHECK (accreditation_avis_year BETWEEN 2000 AND 2100),
    ADD CONSTRAINT program_avis_year_chk CHECK ((accreditation_avis IS NULL) = (accreditation_avis_year IS NULL));

CREATE INDEX establishment_avis_idx ON establishment (habilitation_avis);
CREATE INDEX program_avis_idx ON program (accreditation_avis);

-- -----------------------------------------------------------------------------
-- Publication directe : la personne qui saisit publie ; seule une date de publication est exigée
-- -----------------------------------------------------------------------------
ALTER TABLE news_item
    DROP CONSTRAINT news_published_requires_review_chk,
    DROP CONSTRAINT news_reviewer_is_not_author_chk,
    DROP COLUMN reviewed_by,
    ADD CONSTRAINT news_published_has_date_chk CHECK (status <> 'PUBLIE' OR published_at IS NOT NULL);

ALTER TABLE event
    DROP CONSTRAINT event_published_requires_review_chk,
    DROP CONSTRAINT event_reviewer_is_not_author_chk,
    DROP COLUMN reviewed_by,
    ADD CONSTRAINT event_published_has_date_chk CHECK (status <> 'PUBLIE' OR published_at IS NOT NULL);

ALTER TABLE publication
    DROP CONSTRAINT publication_published_requires_review_chk,
    DROP CONSTRAINT publication_reviewer_is_not_author_chk,
    DROP COLUMN reviewed_by,
    ADD CONSTRAINT publication_published_has_date_chk CHECK (status <> 'PUBLIE' OR published_at IS NOT NULL);

ALTER TABLE institutional_page
    DROP CONSTRAINT page_published_requires_review_chk,
    DROP CONSTRAINT page_reviewer_is_not_author_chk,
    DROP COLUMN reviewed_by,
    DROP COLUMN reviewed_at,
    ADD CONSTRAINT page_published_has_date_chk CHECK (status <> 'PUBLIE' OR published_at IS NOT NULL);
