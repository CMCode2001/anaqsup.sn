-- La chaîne YouTube de l'ANAQ-Sup n'existe plus (ANAQ-Sup, 15/10/2026) : lien retiré des réseaux sociaux du
-- site (bandeau et pied de page) et de la page « Données personnelles et cookies ». YouTube reste proposé dans
-- le back-office si une nouvelle chaîne est créée.

UPDATE site_setting
SET value      = (SELECT coalesce(jsonb_agg(link ORDER BY position), '[]'::jsonb)
                  FROM jsonb_array_elements(value) WITH ORDINALITY AS links(link, position)
                  WHERE link ->> 'network' <> 'youtube'),
    updated_at = now()
WHERE key = 'social'
  AND value @> '[{"network": "youtube"}]';

-- Texte des pages légales, s'il n'a pas été réécrit entre-temps. La version anglaise, rédigée avec la
-- française, reste « à jour » (empreinte recalculée) : DeepL ne la refait pas pour si peu.
WITH changed AS (
    SELECT id,
           replace(body_html, 'Les icônes Facebook, X, LinkedIn et YouTube,', 'Les icônes Facebook, X et LinkedIn,') AS fr,
           replace(body_html_en, 'The Facebook, X, LinkedIn and YouTube icons', 'The Facebook, X and LinkedIn icons') AS en,
           en_auto AND en_source = md5(concat_ws(E'\x1f', title, coalesce(summary, ''), body_html, coalesce(image_role, ''))) AS in_sync
    FROM institutional_page
    WHERE slug = 'donnees-personnelles'
      AND body_html LIKE '%Les icônes Facebook, X, LinkedIn et YouTube,%'
)
UPDATE institutional_page p
SET body_html    = c.fr,
    body_html_en = c.en,
    en_source    = CASE
                       WHEN c.in_sync THEN md5(concat_ws(E'\x1f', p.title, coalesce(p.summary, ''), c.fr, coalesce(p.image_role, '')))
                       ELSE p.en_source
                   END,
    updated_at   = now()
FROM changed c
WHERE p.id = c.id;
