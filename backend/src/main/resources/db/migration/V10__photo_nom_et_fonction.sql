-- Photo d'une page (mot du Secrétaire exécutif) : nom et fonction en deux champs distincts
-- (décision ANAQ-Sup du 14/10/2026). Le nom s'affiche en évidence, la fonction en dessous, sans tiret.
-- Le nom est un nom propre : il n'est pas traduit. La fonction a une version anglaise.

ALTER TABLE institutional_page
    ADD COLUMN image_name    text,
    ADD COLUMN image_role    text,
    ADD COLUMN image_role_en text;

-- Reprise des légendes « Nom — Fonction » (tiret long, tiret court ou plusieurs espaces) ;
-- une légende sans séparateur devient le nom.
UPDATE institutional_page
SET image_name    = coalesce((regexp_match(image_caption, '^\s*(.+?)\s*(?:—|–|\s{2,})\s*(.+?)\s*$'))[1],
                             nullif(btrim(image_caption), ''),
                             (regexp_match(image_caption_en, '^\s*(.+?)\s*(?:—|–|\s{2,})\s*(.+?)\s*$'))[1]),
    image_role    = (regexp_match(image_caption, '^\s*(.+?)\s*(?:—|–|\s{2,})\s*(.+?)\s*$'))[2],
    image_role_en = (regexp_match(image_caption_en, '^\s*(.+?)\s*(?:—|–|\s{2,})\s*(.+?)\s*$'))[2]
WHERE image_caption IS NOT NULL OR image_caption_en IS NOT NULL;

ALTER TABLE institutional_page
    DROP COLUMN image_caption,
    DROP COLUMN image_caption_en;
