-- Ressources documentaires et traduction automatique (décisions ANAQ-Sup du 14/10/2026) :
-- - les documents sont des PDF téléversés depuis le back-office ;
-- - la vignette de chaque document est la première page du PDF, générée au téléversement,
--   remplaçable par une image choisie ;
-- - un contenu sans version anglaise est traduit automatiquement (DeepL) ; la traduction reste modifiable.

-- PDF acceptés dans la médiathèque, en plus des images. Une vignette générée garde le lien vers son PDF.
ALTER TABLE media DROP CONSTRAINT media_content_type_check;
ALTER TABLE media
    ADD CONSTRAINT media_content_type_check
        CHECK (content_type IN ('image/png', 'image/jpeg', 'image/webp', 'application/pdf')),
    ADD COLUMN derived_from uuid REFERENCES media (id) ON DELETE SET NULL;

-- Fichier et vignette d'un document. `file_url` reste pour les liens externes (reprise de l'ancien site).
ALTER TABLE publication
    ADD COLUMN file_key  uuid REFERENCES media (id) ON DELETE SET NULL,
    ADD COLUMN cover_key uuid REFERENCES media (id) ON DELETE SET NULL;

-- Filtre par année de la page « Ressources documentaires ».
CREATE INDEX publication_year_idx ON publication (extract(year FROM published_on)) WHERE status = 'PUBLIE';

-- Traduction automatique. `en_auto` : la version anglaise vient de DeepL et n'a pas été retouchée ;
-- `en_source` : empreinte du texte français traduit. Quand le français change, une traduction
-- automatique est refaite ; une traduction relue ou corrigée dans le back-office (en_auto = false)
-- n'est jamais écrasée.
ALTER TABLE news_item
    ADD COLUMN en_auto   boolean NOT NULL DEFAULT false,
    ADD COLUMN en_source text;
ALTER TABLE event
    ADD COLUMN en_auto   boolean NOT NULL DEFAULT false,
    ADD COLUMN en_source text;
ALTER TABLE publication
    ADD COLUMN en_auto   boolean NOT NULL DEFAULT false,
    ADD COLUMN en_source text;
ALTER TABLE institutional_page
    ADD COLUMN en_auto   boolean NOT NULL DEFAULT false,
    ADD COLUMN en_source text;
