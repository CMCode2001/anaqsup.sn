-- =============================================================================
-- Habilitations et accréditations de DÉMONSTRATION — profil `demo`, développement local uniquement.
-- Établissements et formations FICTIFS (mention « Démo » dans chaque nom) : aucune donnée ne
-- décrit un établissement réel. Idempotent (rejoué si le fichier change).
-- Avis saisis tels quels (favorable / défavorable + année), comme dans le back-office.
-- =============================================================================

INSERT INTO establishment (slug, name, acronym, kind, city, region)
VALUES ('universite-demo-de-dakar', 'Université Démo de Dakar', 'UDD', 'PUBLIC', 'Dakar', 'Dakar'),
       ('universite-demo-de-ziguinchor', 'Université Démo de Ziguinchor', 'UDZ', 'PUBLIC', 'Ziguinchor', 'Ziguinchor'),
       ('institut-superieur-demo-de-thies', 'Institut supérieur Démo de Thiès', 'ISDT', 'PRIVE', 'Thiès', 'Thiès'),
       ('ecole-demo-de-commerce-de-saint-louis', 'École Démo de Commerce de Saint-Louis', 'EDCSL', 'PRIVE', 'Saint-Louis', 'Saint-Louis'),
       ('institut-demo-des-metiers-du-numerique', 'Institut Démo des Métiers du Numérique', 'IDMN', 'PRIVE', 'Dakar', 'Dakar')
ON CONFLICT (slug) DO NOTHING;

UPDATE establishment e
SET habilitation_avis = a.avis, habilitation_avis_year = a.year
FROM (VALUES ('universite-demo-de-dakar', 'FAVORABLE', 2024),
             ('universite-demo-de-ziguinchor', 'FAVORABLE', 2023),
             ('institut-superieur-demo-de-thies', 'FAVORABLE', 2025),
             ('ecole-demo-de-commerce-de-saint-louis', 'DEFAVORABLE', 2025)) AS a(slug, avis, year)
WHERE e.slug = a.slug AND e.habilitation_avis IS NULL;

INSERT INTO establishment_alias (establishment_id, alias, kind)
SELECT e.id, a.alias, a.kind
FROM (VALUES ('universite-demo-de-dakar', 'UDD', 'SIGLE'),
             ('universite-demo-de-ziguinchor', 'UDZ', 'SIGLE'),
             ('institut-superieur-demo-de-thies', 'ISDT', 'SIGLE'),
             ('institut-superieur-demo-de-thies', 'Institut Démo Polytechnique de Thiès', 'ANCIEN_NOM'),
             ('ecole-demo-de-commerce-de-saint-louis', 'EDCSL', 'SIGLE'),
             ('institut-demo-des-metiers-du-numerique', 'IDMN', 'SIGLE')) AS a(slug, alias, kind)
         JOIN establishment e ON e.slug = a.slug
ON CONFLICT DO NOTHING;

INSERT INTO program (slug, establishment_id, title, level, domain)
SELECT p.slug, e.id, p.title, p.level, p.domain
FROM (VALUES ('udd-master-informatique', 'universite-demo-de-dakar', 'Master Informatique', 'MASTER', 'Sciences et technologies'),
             ('udd-licence-droit-prive', 'universite-demo-de-dakar', 'Licence Droit privé', 'LICENCE', 'Sciences juridiques'),
             ('udd-doctorat-sciences-economiques', 'universite-demo-de-dakar', 'Doctorat Sciences économiques', 'DOCTORAT', 'Économie et gestion'),
             ('udz-licence-agronomie', 'universite-demo-de-ziguinchor', 'Licence Agronomie', 'LICENCE', 'Sciences agronomiques'),
             ('udz-master-sciences-environnement', 'universite-demo-de-ziguinchor', 'Master Sciences de l''environnement', 'MASTER', 'Sciences et technologies'),
             ('isdt-licence-genie-civil', 'institut-superieur-demo-de-thies', 'Licence Génie civil', 'LICENCE', 'Sciences de l''ingénieur'),
             ('isdt-bts-electrotechnique', 'institut-superieur-demo-de-thies', 'BTS Électrotechnique', 'BTS', 'Sciences de l''ingénieur'),
             ('edcsl-master-finance', 'ecole-demo-de-commerce-de-saint-louis', 'Master Finance d''entreprise', 'MASTER', 'Économie et gestion'),
             ('edcsl-licence-marketing-digital', 'ecole-demo-de-commerce-de-saint-louis', 'Licence Marketing digital', 'LICENCE', 'Économie et gestion'),
             ('idmn-licence-developpement-web', 'institut-demo-des-metiers-du-numerique', 'Licence Développement web et mobile', 'LICENCE', 'Numérique'),
             ('idmn-dut-reseaux-telecoms', 'institut-demo-des-metiers-du-numerique', 'DUT Réseaux et télécommunications', 'DUT', 'Numérique'))
         AS p(slug, establishment, title, level, domain)
         JOIN establishment e ON e.slug = p.establishment
ON CONFLICT (slug) DO NOTHING;

UPDATE program p
SET accreditation_avis = a.avis, accreditation_avis_year = a.year
FROM (VALUES ('udd-master-informatique', 'FAVORABLE', 2023),
             ('udd-licence-droit-prive', 'FAVORABLE', 2024),
             ('udz-licence-agronomie', 'FAVORABLE', 2022),
             ('isdt-licence-genie-civil', 'FAVORABLE', 2025),
             ('isdt-bts-electrotechnique', 'DEFAVORABLE', 2025),
             ('edcsl-master-finance', 'FAVORABLE', 2021),
             ('idmn-licence-developpement-web', 'FAVORABLE', 2026),
             ('idmn-dut-reseaux-telecoms', 'DEFAVORABLE', 2026)) AS a(slug, avis, year)
WHERE p.slug = a.slug AND p.accreditation_avis IS NULL;
