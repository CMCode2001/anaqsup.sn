-- Reprise des contenus de l'ancien site WordPress (décisions ANAQ-Sup du 14/10/2026) :
-- actualités (flux RSS) et documents PDF (pages Textes réglementaires, Référentiels, Publications).

-- Catégories d'actualités du menu, jusqu'ici créées par les seules données de démonstration :
-- elles doivent exister en production pour recevoir les articles repris.
INSERT INTO news_category (slug, label, label_en, position)
VALUES ('vie-institutionnelle', 'Vie institutionnelle', 'Institutional life', 1),
       ('assurance-qualite', 'Assurance qualité', 'Quality assurance', 2),
       ('cooperation-et-partenariats', 'Coopération & partenariats', 'Cooperation & partnerships', 3),
       ('appels-a-candidatures', 'Appels à candidatures', 'Calls for applications', 4),
       ('communiques', 'Communiqués', 'Press releases', 5)
ON CONFLICT (slug) DO NOTHING;

-- Un document repris ne l'est qu'une fois : son adresse sur l'ancien site sert de clé
-- (et, plus tard, à la redirection de l'ancienne adresse).
ALTER TABLE publication
    ADD COLUMN legacy_url text UNIQUE;
