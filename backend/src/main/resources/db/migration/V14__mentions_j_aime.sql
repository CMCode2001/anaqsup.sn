-- Mentions « J'aime » / « Je n'aime pas » des lecteurs sur les actualités (décision ANAQ-Sup du 14/10/2026).
-- Un vote par navigateur, modifiable ou annulable. Le navigateur est désigné par l'empreinte SHA-256 d'un
-- identifiant anonyme tiré au hasard par le portail (cookie posé au moment du vote) : aucune donnée personnelle.
-- Seul le nombre de « J'aime » est public ; les « Je n'aime pas » ne se lisent que dans le back-office.

CREATE TABLE news_reaction
(
    news_id    uuid        NOT NULL REFERENCES news_item (id) ON DELETE CASCADE,
    visitor    bytea       NOT NULL CHECK (length(visitor) = 32),
    value      smallint    NOT NULL CHECK (value IN (1, -1)),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (news_id, visitor)
);
