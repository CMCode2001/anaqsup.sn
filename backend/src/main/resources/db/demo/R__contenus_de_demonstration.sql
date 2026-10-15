-- =============================================================================
-- Contenus institutionnels de DÉMONSTRATION — profil `demo`, développement local uniquement.
-- Textes génériques et FICTIFS (aucune personne nommée, aucun partenaire réel) destinés à tester la
-- mise en page. Ils seront remplacés par la reprise des contenus officiels (docs/migration).
-- Publication directe par « demo.redaction ».
-- Actualités et documents fictifs : plus créés dès que les contenus de l'ancien site ont été repris
-- (auteur « reprise.wordpress »), pour ne jamais les mêler aux vrais.
-- =============================================================================

-- Rubrique « Coopération & partenariats » du menu (13/10/2026) : ancienne catégorie « Coopération » renommée.
UPDATE news_category SET slug = 'cooperation-et-partenariats', label = 'Coopération & partenariats'
WHERE slug = 'cooperation';

INSERT INTO news_category (slug, label, position)
VALUES ('vie-institutionnelle', 'Vie institutionnelle', 1),
       ('assurance-qualite', 'Assurance qualité', 2),
       ('cooperation-et-partenariats', 'Coopération & partenariats', 3),
       ('appels-a-candidatures', 'Appels à candidatures', 4),
       ('communiques', 'Communiqués', 5)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO news_item (slug, category_id, title, summary, body_html, status, author, published_at)
SELECT n.slug, c.id, n.title, n.summary, n.body, 'PUBLIE', 'demo.redaction', now() - n.age
FROM (VALUES
  ('session-du-conseil-scientifique-demo', 'vie-institutionnelle', 'Session ordinaire du Conseil scientifique',
   'Le Conseil scientifique a examiné les dossiers d''évaluation inscrits à l''ordre du jour.',
   $$<p><em>Contenu de démonstration.</em></p><p>Le Conseil scientifique s'est réuni en session ordinaire pour examiner les rapports d'évaluation externe et formuler ses avis sur les demandes d'habilitation et d'accréditation.</p><p>Les décisions qui en découlent sont publiées dans la rubrique <a href="/habilitations-et-accreditations">Habilitations et accréditations</a>.</p>$$,
   interval '2 days'),
  ('atelier-formation-experts-evaluateurs-demo', 'assurance-qualite', 'Atelier de formation des experts évaluateurs',
   'Trois journées consacrées aux référentiels et à la conduite des visites d''évaluation.',
   $$<p><em>Contenu de démonstration.</em></p><p>Un atelier a réuni de nouveaux experts évaluateurs autour des référentiels d'évaluation, de la rédaction des rapports et de la déontologie de l'évaluation.</p>$$,
   interval '9 days'),
  ('campagne-accreditation-des-programmes-demo', 'appels-a-candidatures', 'Ouverture de la campagne d''accréditation des programmes',
   'Les établissements peuvent déposer leurs dossiers selon le calendrier publié.',
   $$<p><em>Contenu de démonstration.</em></p><p>La campagne d'accréditation des programmes est ouverte. Le calendrier, les pièces à fournir et le référentiel applicable sont disponibles dans la rubrique <a href="/procedures/accreditation">Procédures</a>.</p>$$,
   interval '16 days'),
  ('rencontre-reseau-cellules-qualite-demo', 'assurance-qualite', 'Rencontre du réseau des cellules internes d''assurance qualité',
   'Partage d''expériences entre établissements publics et privés.',
   $$<p><em>Contenu de démonstration.</em></p><p>Les responsables des cellules internes d'assurance qualité ont partagé leurs pratiques d'auto-évaluation et de suivi des recommandations.</p>$$,
   interval '27 days'),
  ('cooperation-regionale-assurance-qualite-demo', 'cooperation-et-partenariats', 'Coopération régionale en assurance qualité',
   'Échanges avec des agences homologues de la sous-région.',
   $$<p><em>Contenu de démonstration.</em></p><p>Une séance de travail a permis d'échanger sur l'harmonisation des pratiques d'évaluation et la reconnaissance mutuelle des décisions.</p>$$,
   interval '41 days'),
  ('communique-calendrier-des-evaluations-demo', 'communiques', 'Communiqué : calendrier des évaluations',
   'Le calendrier prévisionnel des visites d''évaluation est disponible.',
   $$<p><em>Contenu de démonstration.</em></p><p>Le calendrier prévisionnel des visites d'évaluation est publié. Les établissements concernés sont contactés individuellement.</p>$$,
   interval '55 days')
) AS n(slug, category, title, summary, body, age)
JOIN news_category c ON c.slug = n.category
WHERE NOT EXISTS (SELECT 1 FROM news_item WHERE author = 'reprise.wordpress')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO event (slug, title, summary, body_html, location, starts_at, ends_at, status, author, published_at)
SELECT e.slug, e.title, e.summary, e.body, e.location, now() + e.start_in, now() + e.start_in + e.duration,
       'PUBLIE', 'demo.redaction', now() - interval '1 day'
FROM (VALUES
  ('journee-de-la-qualite-demo', 'Journée de la qualité dans l''enseignement supérieur', 'Conférences et tables rondes ouvertes au public.',
   $$<p><em>Contenu de démonstration.</em></p><p>Conférences, tables rondes et présentations de bonnes pratiques en assurance qualité.</p>$$,
   'Dakar', interval '12 days', interval '8 hours'),
  ('seminaire-auto-evaluation-demo', 'Séminaire sur l''auto-évaluation des programmes', 'Pour les responsables de formation et les cellules qualité.',
   $$<p><em>Contenu de démonstration.</em></p><p>Méthodologie de l'auto-évaluation, constitution du dossier et préparation de la visite.</p>$$,
   'Thiès', interval '35 days', interval '2 days'),
  ('colloque-assurance-qualite-demo', 'Colloque international sur l''assurance qualité', 'Trois jours d''échanges entre chercheurs et praticiens.',
   $$<p><em>Contenu de démonstration.</em></p><p>Communications scientifiques et retours d'expérience sur l'évaluation de l'enseignement supérieur et de la recherche.</p>$$,
   'Dakar', interval '-20 days', interval '3 days'),
  ('atelier-referentiels-demo', 'Atelier de présentation des référentiels', 'Présentation des référentiels d''évaluation aux établissements.',
   $$<p><em>Contenu de démonstration.</em></p><p>Présentation des critères et des preuves attendues pour chaque référentiel.</p>$$,
   'Saint-Louis', interval '-60 days', interval '1 day')
) AS e(slug, title, summary, body, location, start_in, duration)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO publication (slug, title, kind, summary, published_on, file_url, status, author, published_at)
SELECT p.slug, p.title, p.kind, p.summary, (now() - p.age)::date, NULL, 'PUBLIE', 'demo.redaction', now() - p.age
FROM (VALUES
  ('rapport-annuel-demo', 'Rapport annuel d''activité (démo)', 'RAPPORT_ANNUEL', 'Bilan des évaluations, décisions et actions de l''année.', interval '30 days'),
  ('referentiel-evaluation-programmes-demo', 'Référentiel d''évaluation des programmes (démo)', 'REFERENTIEL', 'Critères et preuves attendus pour l''accréditation d''une formation.', interval '120 days'),
  ('referentiel-evaluation-institutionnelle-demo', 'Référentiel d''évaluation institutionnelle (démo)', 'REFERENTIEL', 'Critères applicables à l''habilitation des établissements.', interval '150 days'),
  ('guide-auto-evaluation-demo', 'Guide de l''auto-évaluation (démo)', 'GUIDE', 'Méthode pas à pas pour préparer le rapport d''auto-évaluation.', interval '200 days'),
  ('textes-de-reference-demo', 'Textes de référence de l''assurance qualité (démo)', 'TEXTE_REGLEMENTAIRE', 'Recueil des textes encadrant l''habilitation et l''accréditation.', interval '400 days'),
  ('discours-ouverture-colloque-demo', 'Discours d''ouverture du colloque (démo)', 'DISCOURS', 'Allocution prononcée à l''ouverture du colloque.', interval '20 days')
) AS p(slug, title, kind, summary, age)
WHERE NOT EXISTS (SELECT 1 FROM publication WHERE author = 'reprise.wordpress')
ON CONFLICT (slug) DO NOTHING;

-- Ancien slug de démonstration (remplacé par mot-du-secretaire-executif).
DELETE FROM institutional_page WHERE slug = 'mot-de-la-direction';

-- « Gouvernance » n'a plus de version de démonstration : texte officiel installé par la migration V17.
INSERT INTO institutional_page (slug, title, summary, body_html, status, author, published_at)
VALUES
  ('presentation', 'Présentation de l''ANAQ-Sup',
   'L''Autorité nationale d''Assurance Qualité de l''Enseignement supérieur, de la Recherche et de l''Innovation.',
   $$<p><em>Texte de démonstration — à remplacer par le texte officiel validé par l'ANAQ-Sup.</em></p><p>L'ANAQ-Sup veille à la qualité de l'enseignement supérieur, de la recherche et de l'innovation au Sénégal. Elle évalue les établissements et les programmes, et rend publiques ses décisions.</p>$$,
   'PUBLIE', 'demo.redaction', now()),
  ('missions', 'Nos missions', 'Évaluer, habiliter, accréditer et promouvoir la culture de la qualité.',
   $$<p><em>Texte de démonstration — à remplacer par les missions officielles.</em></p><ul><li>Évaluer la qualité des établissements et des programmes.</li><li>Instruire les demandes d'habilitation et d'accréditation et publier les décisions.</li><li>Accompagner les établissements dans leur démarche qualité.</li><li>Promouvoir la culture de la qualité et informer le public.</li></ul>$$,
   'PUBLIE', 'demo.redaction', now()),
  ('mot-du-secretaire-executif', 'Le mot du Secrétaire exécutif', 'Bienvenue sur le portail de l''ANAQ-Sup.',
   $$<p><em>Texte de démonstration — à remplacer par le message officiel.</em></p><p>Bienvenue sur le portail de l'ANAQ-Sup. Ce site présente nos missions, nos actualités et nos événements, et rend accessibles à tous les décisions d'habilitation et d'accréditation.</p><p>La qualité de l'enseignement supérieur est l'affaire de tous : établissements, étudiants, familles et employeurs.</p>$$,
   'PUBLIE', 'demo.redaction', now()),
  ('organigramme', 'Organigramme', 'Organisation des organes et des services de l''ANAQ-Sup.',
   $$<p><em>Organigramme de démonstration — à remplacer par l'organigramme officiel (image ou schéma).</em></p><ul><li><strong>Conseil d'administration</strong></li><li><strong>Conseil scientifique</strong></li><li><strong>Secrétariat exécutif</strong><ul><li>Service de l'évaluation institutionnelle</li><li>Service de l'évaluation des programmes</li><li>Service administratif et financier</li><li>Service de la communication</li></ul></li></ul>$$,
   'PUBLIE', 'demo.redaction', now())
ON CONFLICT (slug) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Versions anglaises de démonstration, pour TOUS les contenus (14/10/2026 : le site anglais
-- doit être complet). En production, un contenu sans version anglaise est traduit
-- automatiquement par DeepL ; la traduction reste modifiable dans le back-office.
-- -----------------------------------------------------------------------------

UPDATE news_category c SET label_en = t.label_en
FROM (VALUES ('vie-institutionnelle', 'Institutional life'),
             ('assurance-qualite', 'Quality assurance'),
             ('cooperation-et-partenariats', 'Cooperation & partnerships'),
             ('appels-a-candidatures', 'Calls for applications'),
             ('communiques', 'Press releases')) AS t(slug, label_en)
WHERE c.slug = t.slug AND c.label_en IS NULL;

UPDATE news_item n SET title_en = t.title_en, summary_en = t.summary_en, body_html_en = t.body_html_en
FROM (VALUES
  ('session-du-conseil-scientifique-demo', 'Ordinary session of the Scientific Council',
   'The Scientific Council reviewed the evaluation files on its agenda.',
   $$<p><em>Demonstration content.</em></p><p>The Scientific Council met in ordinary session to review the external evaluation reports and issue its opinions on applications for institutional approval and programme accreditation.</p><p>The resulting decisions are published in the <a href="/en/approvals-and-accreditations">Approvals &amp; accreditations</a> section.</p>$$),
  ('atelier-formation-experts-evaluateurs-demo', 'Training workshop for expert evaluators',
   'Three days devoted to the quality standards and to conducting evaluation visits.',
   $$<p><em>Demonstration content.</em></p><p>A workshop brought together new expert evaluators to work on the evaluation standards, report writing and the ethics of evaluation.</p>$$),
  ('communique-calendrier-des-evaluations-demo', 'Press release: evaluation schedule',
   'The provisional schedule of evaluation visits is available.',
   $$<p><em>Demonstration content.</em></p><p>The provisional schedule of evaluation visits has been published. The institutions concerned are contacted individually.</p>$$),
  ('campagne-accreditation-des-programmes-demo', 'Programme accreditation campaign now open',
   'Institutions can submit their applications according to the published schedule.',
   $$<p><em>Demonstration content.</em></p><p>The programme accreditation campaign is open. The schedule, the required documents and the applicable quality standard are available in the <a href="/en/procedures/programme-accreditation">Procedures</a> section.</p>$$),
  ('rencontre-reseau-cellules-qualite-demo', 'Meeting of the network of internal quality assurance units',
   'Sharing of experience between public and private institutions.',
   $$<p><em>Demonstration content.</em></p><p>The heads of the internal quality assurance units shared their self-assessment practices and their follow-up of recommendations.</p>$$),
  ('cooperation-regionale-assurance-qualite-demo', 'Regional cooperation in quality assurance',
   'Exchanges with peer agencies in the sub-region.',
   $$<p><em>Demonstration content.</em></p><p>A working session provided an opportunity to discuss the harmonisation of evaluation practices and the mutual recognition of decisions.</p>$$)
) AS t(slug, title_en, summary_en, body_html_en)
WHERE n.slug = t.slug AND n.title_en IS NULL;

UPDATE event e SET title_en = t.title_en, summary_en = t.summary_en, body_html_en = t.body_html_en
FROM (VALUES
  ('journee-de-la-qualite-demo', 'Quality in Higher Education Day', 'Lectures and round tables open to the public.',
   $$<p><em>Demonstration content.</em></p><p>Lectures, round tables and presentations of good practice in quality assurance.</p>$$),
  ('colloque-assurance-qualite-demo', 'International conference on quality assurance', 'Three days of exchanges between researchers and practitioners.',
   $$<p><em>Demonstration content.</em></p><p>Scientific papers and lessons learned on the evaluation of higher education and research.</p>$$),
  ('seminaire-auto-evaluation-demo', 'Seminar on programme self-assessment', 'For programme leaders and quality units.',
   $$<p><em>Demonstration content.</em></p><p>Self-assessment methodology, preparing the application file and getting ready for the evaluation visit.</p>$$),
  ('atelier-referentiels-demo', 'Workshop presenting the quality standards', 'Presentation of the evaluation standards to institutions.',
   $$<p><em>Demonstration content.</em></p><p>Presentation of the criteria and of the evidence expected for each quality standard.</p>$$)
) AS t(slug, title_en, summary_en, body_html_en)
WHERE e.slug = t.slug AND e.title_en IS NULL;

UPDATE publication p SET title_en = t.title_en, summary_en = t.summary_en
FROM (VALUES
  ('rapport-annuel-demo', 'Annual activity report (demo)', 'Review of the year''s evaluations, decisions and actions.'),
  ('referentiel-evaluation-programmes-demo', 'Programme evaluation standards (demo)', 'Criteria and evidence required for programme accreditation.'),
  ('guide-auto-evaluation-demo', 'Self-assessment guide (demo)', 'Step-by-step method for preparing the self-assessment report.'),
  ('textes-de-reference-demo', 'Reference texts on quality assurance (demo)', 'Collection of the texts governing institutional approval and accreditation.'),
  ('referentiel-evaluation-institutionnelle-demo', 'Institutional evaluation standards (demo)', 'Criteria applicable to the approval of institutions.'),
  ('discours-ouverture-colloque-demo', 'Opening speech at the conference (demo)', 'Address delivered at the opening of the conference.')
) AS t(slug, title_en, summary_en)
WHERE p.slug = t.slug AND p.title_en IS NULL;

UPDATE institutional_page ip SET title_en = t.title_en, summary_en = t.summary_en, body_html_en = t.body_html_en
FROM (VALUES
  ('presentation', 'About ANAQ-Sup',
   'The National Authority for Quality Assurance in Higher Education, Research and Innovation.',
   $$<p><em>Demonstration text — to be replaced by the official text approved by ANAQ-Sup.</em></p><p>ANAQ-Sup ensures the quality of higher education, research and innovation in Senegal. It evaluates institutions and programmes, and makes its decisions public.</p>$$),
  ('missions', 'Our missions', 'Evaluating, approving, accrediting and promoting a culture of quality.',
   $$<p><em>Demonstration text — to be replaced by the official missions.</em></p><ul><li>Evaluate the quality of institutions and programmes.</li><li>Process applications for institutional approval and programme accreditation, and publish the decisions.</li><li>Support institutions in their quality approach.</li><li>Promote a culture of quality and inform the public.</li></ul>$$),
  ('organigramme', 'Organisation chart', 'How ANAQ-Sup''s bodies and departments are organised.',
   $$<p><em>Demonstration organisation chart — to be replaced by the official chart (image or diagram).</em></p><ul><li><strong>Board of Directors</strong></li><li><strong>Scientific Council</strong></li><li><strong>Executive Secretariat</strong><ul><li>Institutional evaluation department</li><li>Programme evaluation department</li><li>Administration and finance department</li><li>Communication department</li></ul></li></ul>$$),
  ('mot-du-secretaire-executif', 'A word from the Executive Secretary', 'Welcome to the ANAQ-Sup portal.',
   $$<p><em>Demonstration text — to be replaced by the official message.</em></p><p>Welcome to the ANAQ-Sup portal. This site presents our missions, news and events, and makes institutional approval and accreditation decisions available to everyone.</p><p>The quality of higher education is everyone's business: institutions, students, families and employers.</p>$$)
) AS t(slug, title_en, summary_en, body_html_en)
WHERE ip.slug = t.slug AND ip.title_en IS NULL;

-- -----------------------------------------------------------------------------
-- Images mises en avant de démonstration : illustrations abstraites aux couleurs du logotype
-- (frontend/apps/portail/public/demo/actualites/), pas de vraies photos sous des articles fictifs.
-- Un article est épinglé « À la une » pour montrer la règle : épinglé, sinon le plus récent.
-- -----------------------------------------------------------------------------

UPDATE news_item SET cover_image_key = '/demo/actualites/' || slug || '.svg'
WHERE slug LIKE '%-demo' AND cover_image_key IS NULL;

UPDATE news_item SET featured = true
WHERE slug = 'campagne-accreditation-des-programmes-demo'
  AND NOT EXISTS (SELECT 1 FROM news_item WHERE featured);

-- -----------------------------------------------------------------------------
-- Mot du Secrétaire exécutif sur l'accueil (13/10/2026) : nom et fonction FICTIFS, texte d'un paragraphe.
-- La photo officielle est à téléverser dans le back-office (Pages institutionnelles).
-- -----------------------------------------------------------------------------

UPDATE institutional_page
SET image_name    = 'Prénom NOM (démonstration)',
    image_role    = 'Secrétaire exécutif de l''ANAQ-Sup',
    image_role_en = 'Executive Secretary of ANAQ-Sup'
WHERE slug = 'mot-du-secretaire-executif' AND image_name IS NULL AND image_role IS NULL;

UPDATE institutional_page
SET summary    = 'Bienvenue sur le portail de l''ANAQ-Sup. Notre mission est de garantir la qualité de l''enseignement supérieur, de la recherche et de l''innovation au Sénégal : nous évaluons les établissements et les formations, nous publions nos avis en toute transparence et nous accompagnons les établissements dans leur démarche qualité, au service des étudiants, des familles et des employeurs. (Texte de démonstration.)',
    summary_en = 'Welcome to the ANAQ-Sup portal. Our mission is to guarantee the quality of higher education, research and innovation in Senegal: we evaluate institutions and programmes, publish our opinions transparently and support institutions in their quality approach, for the benefit of students, families and employers. (Demonstration text.)'
WHERE slug = 'mot-du-secretaire-executif' AND summary = 'Bienvenue sur le portail de l''ANAQ-Sup.';
