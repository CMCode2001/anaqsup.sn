-- =============================================================================
-- V17 — Habilitations et accréditations : décisions, rapports, composantes.
--
-- Chaque décision du Conseil scientifique est conservée (historique) avec son rapport d'évaluation.
-- Les colonnes habilitation_avis(_year) et accreditation_avis(_year) créées par V4 NE SONT PLUS SAISIES :
-- elles reflètent la décision validée la plus récente et sont tenues à jour par des déclencheurs.
-- Le portail, la recherche et les statistiques continuent donc de lire les mêmes colonnes.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Composantes : UFR, facultés et écoles doctorales sont des établissements enfants.
-- -----------------------------------------------------------------------------
ALTER TABLE establishment
    ADD COLUMN parent_id             uuid REFERENCES establishment (id),
    ADD COLUMN nature                text    NOT NULL DEFAULT 'AUTRE'
        CHECK (nature IN ('UNIVERSITE', 'INSTITUT', 'ECOLE', 'CENTRE', 'UFR_FACULTE', 'ECOLE_DOCTORALE', 'AUTRE')),
    -- vrai quand une décision plus récente est en cours de vérification : l'avis affiché est provisoire
    ADD COLUMN verification_en_cours boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT establishment_parent_not_self_chk CHECK (parent_id IS NULL OR parent_id <> id);

CREATE INDEX establishment_parent_idx ON establishment (parent_id);

-- Une composante a le même type (public ou privé) que son établissement.
CREATE FUNCTION establishment_check_parent() RETURNS trigger
    LANGUAGE plpgsql AS
$$
DECLARE
    parent_kind text;
BEGIN
    IF NEW.parent_id IS NOT NULL THEN
        SELECT kind INTO parent_kind FROM establishment WHERE id = NEW.parent_id;
        IF parent_kind IS DISTINCT FROM NEW.kind THEN
            RAISE EXCEPTION 'Le type (%) d''une composante doit être celui de son établissement (%)', NEW.kind, parent_kind
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER establishment_check_parent_trg
    BEFORE INSERT OR UPDATE OF parent_id, kind
    ON establishment
    FOR EACH ROW
EXECUTE FUNCTION establishment_check_parent();

-- -----------------------------------------------------------------------------
-- Formations : option de parcours, un programme n'existe qu'une fois.
-- Les niveaux (LICENCE, MASTER, DOCTORAT, DUT, BTS, AUTRE) et les avis affichés (FAVORABLE, DEFAVORABLE) restent ceux
-- du contrat public : le frontend est typé dessus. Une accréditation « conditionnelle » s'affiche FAVORABLE sur la fiche ;
-- sa nature exacte (CONDITIONNEL) est portée par la décision de l'historique.
-- -----------------------------------------------------------------------------
ALTER TABLE program
    ADD COLUMN option_parcours text,
    ADD COLUMN verification_en_cours boolean NOT NULL DEFAULT false;

-- « option Assurance » et « option Ingénierie financière » sont deux programmes.
CREATE UNIQUE INDEX program_unique_idx ON program (establishment_id, immutable_unaccent(lower(title)), level,
                                                   (immutable_unaccent(lower(coalesce(option_parcours, '')))));

-- -----------------------------------------------------------------------------
-- Rapports d'évaluation (PDF publiés sur anaqsup.sn)
-- -----------------------------------------------------------------------------
CREATE TABLE report
(
    id              uuid PRIMARY KEY     DEFAULT uuidv7(),
    kind            text        NOT NULL DEFAULT 'RAPPORT_EVALUATION_EXTERNE'
        CHECK (kind IN ('RAPPORT_EVALUATION_EXTERNE', 'DECISION_CONSEIL_SCIENTIFIQUE', 'AUTRE')),
    title           text        NOT NULL,  -- titre lu sur la page de garde du PDF, pas le nom du fichier
    url             text        NOT NULL UNIQUE CHECK (url ~ '^https?://\S+$'),
    sha256          text CHECK (sha256 ~ '^[a-f0-9]{64}$'),
    signed_on       date,
    visited_on      date,
    -- Un rapport absent du site reste privé : il n'est jamais exposé par l'API publique.
    visibility      text        NOT NULL DEFAULT 'PUBLIC' CHECK (visibility IN ('PUBLIC', 'PRIVE')),
    link_ok         boolean,               -- résultat du dernier contrôle du lien
    link_checked_at timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- Décisions du Conseil scientifique : historique complet, statut courant calculé.
-- -----------------------------------------------------------------------------
CREATE TABLE decision
(
    id                  uuid PRIMARY KEY     DEFAULT uuidv7(),
    kind                text        NOT NULL
        CHECK (kind IN ('HABILITATION', 'HABILITATION_COMPLEMENTAIRE', 'ACCREDITATION', 'RENOUVELLEMENT_ACCREDITATION')),
    establishment_id    uuid        NOT NULL REFERENCES establishment (id),
    decided_on          date,                  -- date du Conseil scientifique ; NULL tant qu'elle n'est pas connue
    display_year        smallint    NOT NULL CHECK (display_year BETWEEN 2014 AND 2100),
    validity_years      smallint    NOT NULL CHECK (validity_years BETWEEN 1 AND 20),
    wave                smallint,
    avis                text CHECK (avis IN ('FAVORABLE', 'DEFAVORABLE', 'CONDITIONNEL')), -- NULL si donné programme par programme
    report_id           uuid REFERENCES report (id),
    previous_id         uuid REFERENCES decision (id),
    status              text        NOT NULL DEFAULT 'BROUILLON' CHECK (status IN ('BROUILLON', 'VALIDE', 'ARCHIVE')),
    comment             text,
    source_ref          text,                  -- ligne d'origine sur l'ancien site (traçabilité de la reprise)
    -- SAISIE_RAPIDE : avis et année saisis depuis la fiche, sans rapport ; une telle décision se supprime avec sa fiche.
    origin              text        NOT NULL DEFAULT 'SAISIE_BACKOFFICE'
        CHECK (origin IN ('SAISIE_BACKOFFICE', 'SAISIE_RAPIDE', 'REPRISE_WORDPRESS')),
    entered_by          text        NOT NULL,
    validated_by        text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT decision_year_matches_date_chk CHECK (decided_on IS NULL OR display_year = EXTRACT(YEAR FROM decided_on)),
    CONSTRAINT decision_previous_not_self_chk CHECK (previous_id IS NULL OR previous_id <> id),
    CONSTRAINT decision_validated_has_validator_chk CHECK (status = 'BROUILLON' OR validated_by IS NOT NULL)
);

CREATE INDEX decision_establishment_idx ON decision (establishment_id, display_year DESC);
CREATE UNIQUE INDEX decision_source_ref_idx ON decision (source_ref) WHERE source_ref IS NOT NULL;
CREATE INDEX decision_status_idx ON decision (status, kind);

-- Avis programme par programme : gère les avis mixtes et les décisions collectives
-- (une école doctorale, un seul rapport, plusieurs doctorats).
CREATE TABLE decision_program
(
    decision_id uuid NOT NULL REFERENCES decision (id) ON DELETE CASCADE,
    program_id  uuid NOT NULL REFERENCES program (id),
    avis        text NOT NULL CHECK (avis IN ('FAVORABLE', 'DEFAVORABLE', 'CONDITIONNEL')),
    report_id   uuid REFERENCES report (id), -- rapport propre au programme, sinon celui de la décision
    remark      text,
    PRIMARY KEY (decision_id, program_id)
);

CREATE INDEX decision_program_program_idx ON decision_program (program_id);

-- Règles de cohérence, filet de sécurité derrière les contrôles de l'API.
CREATE FUNCTION decision_check() RETURNS trigger
    LANGUAGE plpgsql AS
$$
DECLARE
    signed date;
BEGIN
    NEW.updated_at := now();
    IF NEW.decided_on > current_date OR NEW.display_year > EXTRACT(YEAR FROM current_date) THEN
        RAISE EXCEPTION 'Décision dans le futur : % / %', NEW.decided_on, NEW.display_year
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    IF NEW.report_id IS NOT NULL THEN
        SELECT signed_on INTO signed FROM report WHERE id = NEW.report_id;
        IF signed IS NOT NULL AND NEW.decided_on IS NOT NULL AND NEW.decided_on < signed THEN
            RAISE EXCEPTION 'Date du CS (%) antérieure à la signature du rapport (%)', NEW.decided_on, signed
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        IF signed IS NOT NULL AND NEW.decided_on IS NULL AND NEW.display_year < EXTRACT(YEAR FROM signed) THEN
            RAISE EXCEPTION 'Année (%) antérieure à la signature du rapport (%)', NEW.display_year, signed
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER decision_check_trg
    BEFORE INSERT OR UPDATE
    ON decision
    FOR EACH ROW
EXECUTE FUNCTION decision_check();

-- Le programme évalué appartient à l'établissement de la décision ou à l'une de ses composantes.
CREATE FUNCTION decision_program_check() RETURNS trigger
    LANGUAGE plpgsql AS
$$
DECLARE
    decision_establishment uuid;
    allowed                boolean;
BEGIN
    SELECT establishment_id INTO decision_establishment FROM decision WHERE id = NEW.decision_id;
    WITH RECURSIVE up AS (SELECT e.id, e.parent_id
                          FROM establishment e
                                   JOIN program p ON p.establishment_id = e.id
                          WHERE p.id = NEW.program_id
                          UNION ALL
                          SELECT e.id, e.parent_id
                          FROM establishment e
                                   JOIN up ON e.id = up.parent_id)
    SELECT EXISTS (SELECT 1 FROM up WHERE id = decision_establishment) INTO allowed;
    IF NOT allowed THEN
        RAISE EXCEPTION 'Le programme % n''appartient pas à l''établissement de la décision', NEW.program_id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER decision_program_check_trg
    BEFORE INSERT OR UPDATE
    ON decision_program
    FOR EACH ROW
EXECUTE FUNCTION decision_program_check();

-- -----------------------------------------------------------------------------
-- Statut courant : la décision validée la plus récente alimente les colonnes d'avis des fiches.
-- -----------------------------------------------------------------------------
CREATE FUNCTION refresh_establishment_avis(p_id uuid) RETURNS void
    LANGUAGE sql AS
$$
UPDATE establishment e
SET habilitation_avis      = x.avis,
    habilitation_avis_year = x.display_year,
    updated_at             = now()
FROM (SELECT e2.id, CASE d.avis WHEN 'CONDITIONNEL' THEN 'FAVORABLE' ELSE d.avis END AS avis, d.display_year
      FROM establishment e2
               LEFT JOIN LATERAL (SELECT d.avis, d.display_year
                                  FROM decision d
                                  WHERE d.establishment_id = e2.id AND d.kind = 'HABILITATION' AND d.status = 'VALIDE'
                                    AND d.avis IS NOT NULL
                                  ORDER BY coalesce(d.decided_on, make_date(d.display_year, 12, 31)) DESC, d.created_at DESC
                                  LIMIT 1) d ON true
      WHERE e2.id = p_id) x
WHERE e.id = x.id
  AND (e.habilitation_avis IS DISTINCT FROM x.avis OR e.habilitation_avis_year IS DISTINCT FROM x.display_year);
$$;

CREATE FUNCTION refresh_program_avis(p_id uuid) RETURNS void
    LANGUAGE sql AS
$$
UPDATE program p
SET accreditation_avis      = x.avis,
    accreditation_avis_year = x.display_year,
    updated_at              = now()
FROM (SELECT p2.id, CASE d.avis WHEN 'CONDITIONNEL' THEN 'FAVORABLE' ELSE d.avis END AS avis, d.display_year
      FROM program p2
               LEFT JOIN LATERAL (SELECT dp.avis, d.display_year
                                  FROM decision_program dp
                                           JOIN decision d ON d.id = dp.decision_id AND d.status = 'VALIDE'
                                  WHERE dp.program_id = p2.id
                                  ORDER BY coalesce(d.decided_on, make_date(d.display_year, 12, 31)) DESC, d.created_at DESC
                                  LIMIT 1) d ON true
      WHERE p2.id = p_id) x
WHERE p.id = x.id
  AND (p.accreditation_avis IS DISTINCT FROM x.avis OR p.accreditation_avis_year IS DISTINCT FROM x.display_year);
$$;

CREATE FUNCTION decision_refresh_avis() RETURNS trigger
    LANGUAGE plpgsql AS
$$
DECLARE
    pid uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        PERFORM refresh_establishment_avis(OLD.establishment_id);
        FOR pid IN SELECT program_id FROM decision_program WHERE decision_id = OLD.id
            LOOP
                PERFORM refresh_program_avis(pid);
            END LOOP;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        PERFORM refresh_establishment_avis(NEW.establishment_id);
        FOR pid IN SELECT program_id FROM decision_program WHERE decision_id = NEW.id
            LOOP
                PERFORM refresh_program_avis(pid);
            END LOOP;
    END IF;
    RETURN NULL;
END
$$;

CREATE TRIGGER decision_refresh_avis_trg
    AFTER INSERT OR UPDATE OR DELETE
    ON decision
    FOR EACH ROW
EXECUTE FUNCTION decision_refresh_avis();

CREATE FUNCTION decision_program_refresh_avis() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF TG_OP <> 'INSERT' THEN
        PERFORM refresh_program_avis(OLD.program_id);
    END IF;
    IF TG_OP <> 'DELETE' THEN
        PERFORM refresh_program_avis(NEW.program_id);
    END IF;
    RETURN NULL;
END
$$;

CREATE TRIGGER decision_program_refresh_avis_trg
    AFTER INSERT OR UPDATE OR DELETE
    ON decision_program
    FOR EACH ROW
EXECUTE FUNCTION decision_program_refresh_avis();

-- -----------------------------------------------------------------------------
-- Reprise de l'ancien site : chaque ligne lue est conservée avec son résultat.
-- IMPORTEE : entrée dans la base. A_VERIFIER : au moins une anomalie, elle n'y est pas entrée.
-- -----------------------------------------------------------------------------
CREATE TABLE import_line
(
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch       text        NOT NULL,
    page        text        NOT NULL CHECK (page IN ('HABILITATIONS', 'ACCREDITATIONS')),
    line_number integer     NOT NULL,
    content     jsonb       NOT NULL,          -- la ligne telle qu'elle est sur l'ancien site
    status      text        NOT NULL CHECK (status IN ('IMPORTEE', 'A_VERIFIER')),
    anomalies   jsonb       NOT NULL DEFAULT '[]'::jsonb,
    decision_id uuid REFERENCES decision (id),
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (batch, page, line_number)
);

CREATE INDEX import_line_status_idx ON import_line (batch, status);
