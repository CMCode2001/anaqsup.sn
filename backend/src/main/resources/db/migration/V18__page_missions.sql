-- Page « Missions » : texte officiel de l'ancien site (anaqsup.sn/missions, repris le 15/10/2026), à la place du texte
-- de démonstration ; une mission par intertitre (cartes du portail), dans l'ordre de lecture de l'ancienne page.
-- Typographie corrigée : accents des intertitres (« Évaluation »), minuscules, « n° », dates, « ; » final des
-- éléments de liste devenu un point (chaque mission est une carte), « auto- évaluation » → « auto-évaluation ».
-- La version anglaise, rédigée avec la française, est tenue pour une traduction automatique à jour. Une page déjà
-- rédigée dans le back-office n'est pas touchée.

INSERT INTO institutional_page (slug, title, summary, body_html, title_en, summary_en, body_html_en, status, author, published_at)
VALUES ('missions',
        'Missions de l''ANAQ-Sup',
        'L’ANAQ-Sup est une agence de l’État sous la tutelle administrative et technique du Ministère de l’Enseignement supérieur, de la Recherche et de l’Innovation (MESRI).',
        $fr$<p>Elle a été créée par le décret n° 2012-837 du 7 août 2012, remplacé par le nouveau décret n° 2018-1956 du 7 novembre 2018 élargissant ses missions à la Recherche et à l’Innovation.</p>
<p>L’ANAQ-Sup a pour mission :</p>
<h2>Standards de qualité</h2>
<p>Définir, en rapport avec le Ministère chargé de l’Enseignement supérieur, de la Recherche et de l’Innovation, les établissements d’enseignement supérieur et les centres ou organismes de recherche et d’innovation, les standards de qualité à respecter dans l’exécution de leurs missions.</p>
<h2>Système d’assurance qualité</h2>
<p>Concevoir et mettre en place un système d’assurance qualité compatible avec les objectifs et les exigences de l’enseignement supérieur, de la recherche et de l’innovation.</p>
<h2>Procédures formelles</h2>
<p>Mettre en place des procédures formelles et identifier les critères pour l’évaluation de la qualité des établissements d’enseignement supérieur et des centres ou organismes de recherche et d’innovation.</p>
<h2>Avis technique</h2>
<p>Donner un avis technique aux Ministres compétents sur les demandes d’habilitation ou d’accréditation des établissements d’enseignement supérieur, des centres et organismes de recherche et d’innovation.</p>
<h2>Évaluation des établissements</h2>
<p>Évaluer périodiquement les établissements d’enseignement supérieur publics et privés, les centres et les organismes de recherche et d’innovation. Assister et accompagner les établissements d’enseignement supérieur, les centres et les organismes de recherche et d’innovation dans le développement et la mise en œuvre de leurs procédures internes d’assurance qualité et d’auto-évaluation.</p>
<h2>Évaluation des programmes de formation</h2>
<p>Évaluer périodiquement les programmes de formation ou d’études des établissements, notamment les éléments relatifs aux enseignements, aux outils et aux méthodes pédagogiques.</p>$fr$,
        'Missions of ANAQ-Sup',
        'ANAQ-Sup is a State agency under the administrative and technical supervision of the Ministry of Higher Education, Research and Innovation (MESRI).',
        $en$<p>It was established by Decree No. 2012-837 of 7 August 2012, replaced by the new Decree No. 2018-1956 of 7 November 2018, which extended its missions to research and innovation.</p>
<p>ANAQ-Sup’s missions are to:</p>
<h2>Quality standards</h2>
<p>Define, in consultation with the Ministry in charge of Higher Education, Research and Innovation, higher education institutions and research and innovation centres or bodies, the quality standards to be met in carrying out their missions.</p>
<h2>Quality assurance system</h2>
<p>Design and implement a quality assurance system consistent with the objectives and requirements of higher education, research and innovation.</p>
<h2>Formal procedures</h2>
<p>Establish formal procedures and identify the criteria for assessing the quality of higher education institutions and research and innovation centres or bodies.</p>
<h2>Technical opinion</h2>
<p>Give a technical opinion to the competent Ministers on applications for the approval or accreditation of higher education institutions and of research and innovation centres and bodies.</p>
<h2>Evaluation of institutions</h2>
<p>Periodically evaluate public and private higher education institutions and research and innovation centres and bodies. Assist and support higher education institutions and research and innovation centres and bodies in developing and implementing their internal quality assurance and self-assessment procedures.</p>
<h2>Evaluation of study programmes</h2>
<p>Periodically evaluate institutions’ study and training programmes, in particular teaching, tools and teaching methods.</p>$en$,
        'PUBLIE', 'migration', now())
ON CONFLICT (slug) DO UPDATE
SET title        = EXCLUDED.title,
    summary      = EXCLUDED.summary,
    body_html    = EXCLUDED.body_html,
    title_en     = EXCLUDED.title_en,
    summary_en   = EXCLUDED.summary_en,
    body_html_en = EXCLUDED.body_html_en,
    status       = EXCLUDED.status,
    author       = EXCLUDED.author,
    updated_at   = now()
WHERE institutional_page.author = 'demo.redaction';

UPDATE institutional_page
SET en_auto   = true,
    en_source = md5(concat_ws(E'\x1f', title, coalesce(summary, ''), body_html, coalesce(image_role, '')))
WHERE slug = 'missions'
  AND author = 'migration';
