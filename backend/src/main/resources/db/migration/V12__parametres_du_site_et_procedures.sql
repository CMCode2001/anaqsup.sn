-- Tout ce qui s'affiche sur le site est administrable (décision ANAQ-Sup du 14/10/2026) :
-- coordonnées, réseaux sociaux, liens utiles du pied de page et textes d'introduction des rubriques
-- (« paramètres du site »), et les deux fiches procédures, devenues des pages éditables.

CREATE TABLE site_setting
(
    key        text PRIMARY KEY CHECK (key IN ('contact', 'social', 'useful_links', 'intros')),
    value      jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by text
);

-- Valeurs actuelles du site (fournies par l'ANAQ-Sup le 13/10/2026), modifiables ensuite.
INSERT INTO site_setting (key, value)
VALUES ('contact', '{
          "phone": "+221 33 825 75 18",
          "email": "secretariat@anaqsup.sn",
          "address": "Résidence Adja Aby Gueye, Sacré-Cœur 3 Pyrotechnie, Lot N° R126, Dakar",
          "addressEn": null
        }'),
       ('social', '[
          {"network": "facebook", "url": "https://www.facebook.com/ANAQSup/"},
          {"network": "x", "url": "https://x.com/ANAQSUP"},
          {"network": "linkedin", "url": "https://www.linkedin.com/company/anaq-sup/"},
          {"network": "youtube", "url": "https://www.youtube.com/channel/UCUFWBm9s_oO1i5-_ihzdWiA"}
        ]'),
       ('useful_links', '[
          {"label": "MESRI", "labelEn": null, "description": "Ministère de l''Enseignement supérieur, de la Recherche et de l''Innovation",
           "descriptionEn": "Ministry of Higher Education, Research and Innovation", "url": "https://mesrisenegal.sn"},
          {"label": "CAMES", "labelEn": null, "description": "Conseil africain et malgache pour l''enseignement supérieur",
           "descriptionEn": "African and Malagasy Council for Higher Education", "url": "https://www.lecames.org"},
          {"label": "CAMPUSEN", "labelEn": null, "description": "Plateforme d''orientation des nouveaux bacheliers",
           "descriptionEn": "Guidance platform for new baccalaureate holders", "url": "https://www.campusen.sn"},
          {"label": "UNESCO", "labelEn": null, "description": "Bureau régional de l''UNESCO à Dakar",
           "descriptionEn": "UNESCO Regional Office in Dakar", "url": "https://www.unesco.org/fr/fieldoffice/dakar"}
        ]'),
       ('intros', '{}')
ON CONFLICT (key) DO NOTHING;

-- Permission « Paramètres du site », accordée à l'Administrateur (à donner à d'autres rôles au besoin).
ALTER TABLE admin_role DROP CONSTRAINT admin_role_permissions_check;
ALTER TABLE admin_role ADD CONSTRAINT admin_role_permissions_check CHECK (
    permissions <@ ARRAY ['COMPTES', 'ACTUALITES', 'EVENEMENTS', 'DOCUMENTS', 'PAGES', 'AVIS', 'PARTENAIRES', 'PARAMETRES']::text[]);
UPDATE admin_role SET permissions = array_append(permissions, 'PARAMETRES')
WHERE system AND NOT ('PARAMETRES' = ANY (permissions));

-- Fiches procédures : pages éditables (texte à fournir par l'ANAQ-Sup), à la place du gabarit figé.
INSERT INTO institutional_page (slug, title, summary, body_html, status, author, published_at)
VALUES ('procedure-habilitation', 'Habilitation d''un établissement',
        'Autorisation pour un établissement de délivrer des diplômes.',
        '<h2>À qui s''adresse cette procédure</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Conditions d''éligibilité</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Étapes</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Pièces à fournir</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Délais indicatifs</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Résultats possibles</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Textes et référentiels de référence</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>',
        'PUBLIE', 'migration', now()),
       ('procedure-accreditation', 'Accréditation d''un programme',
        'Reconnaissance de la qualité d''une formation.',
        '<h2>À qui s''adresse cette procédure</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Conditions d''éligibilité</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Étapes</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Pièces à fournir</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Délais indicatifs</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Résultats possibles</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>'
            || '<h2>Textes et référentiels de référence</h2><p><em>Contenu à fournir et à valider par l''ANAQ-Sup.</em></p>',
        'PUBLIE', 'migration', now())
ON CONFLICT (slug) DO NOTHING;
