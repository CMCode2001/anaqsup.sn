-- Page « Gouvernance » : texte officiel de l'ancien site (anaqsup.sn/organe-de-gouvernance, repris le 15/10/2026),
-- à la place du texte de démonstration. Typographie corrigée (espaces, « ANAQ-Sup », points finaux, intertitres en
-- minuscules) et trois fautes de l'original, corrections validées par l'ANAQ-Sup : phrase sans verbe du Conseil
-- scientifique, « nommée » → « nommé », « et de l'innovation » → « et l'innovation ». La version anglaise, rédigée avec la française, est tenue pour une traduction
-- automatique à jour. Une page déjà rédigée dans le back-office n'est pas touchée.

INSERT INTO institutional_page (slug, title, summary, body_html, title_en, summary_en, body_html_en, status, author, published_at)
VALUES ('gouvernance',
        'Gouvernance de l''ANAQ-Sup',
        'L’ANAQ-Sup comprend trois organes de gouvernance : le Conseil d’administration, le Conseil scientifique et le Secrétariat exécutif.',
        $fr$<h2>Le Conseil d’administration</h2>
<p>Le Conseil d’administration assure la supervision des activités de l’Autorité, en application des orientations et de la politique de l’État dans le domaine de l’Assurance Qualité dans l’enseignement supérieur, la recherche et l’innovation. Il vote le budget ou les comptes prévisionnels annuels de l’ANAQ-Sup.</p>
<h2>Le Conseil scientifique</h2>
<p>Les membres du Conseil scientifique sont des personnalités reconnues pour leur connaissance du sous-secteur, leur expertise professionnelle et, autant que possible, leur connaissance de l’assurance qualité de l’enseignement supérieur. Ils sont désignés par le MESRI à partir d’une liste restreinte proposée par le Secrétaire exécutif.</p>
<h2>Le Secrétariat exécutif</h2>
<p>L’ANAQ-Sup est administrée par un Secrétaire exécutif nommé par décret sur proposition du MESRI. Le SE est investi du pouvoir de décision nécessaire à la bonne marche de l’ANAQ-Sup et veille à l’exécution des décisions du CA et du CS. Pour l’exécution de ses missions, le Secrétaire exécutif s’appuie sur une équipe composée d’agents administratifs, techniques et de support. L’ANAQ-Sup est membre de plusieurs réseaux internationaux d’assurance qualité dont le Réseau africain francophone d’assurance qualité (RAFANAQ).</p>$fr$,
        'Governance of ANAQ-Sup',
        'ANAQ-Sup has three governing bodies: the Board of Directors, the Scientific Council and the Executive Secretariat.',
        $en$<h2>The Board of Directors</h2>
<p>The Board of Directors oversees the Authority’s activities, in line with the State’s guidelines and policy on quality assurance in higher education, research and innovation. It votes on ANAQ-Sup’s annual budget or forecast accounts.</p>
<h2>The Scientific Council</h2>
<p>The members of the Scientific Council are figures recognised for their knowledge of the sub-sector, their professional expertise and, as far as possible, their knowledge of quality assurance in higher education. They are appointed by the MESRI (Ministry of Higher Education, Research and Innovation) from a shortlist proposed by the Executive Secretary.</p>
<h2>The Executive Secretariat</h2>
<p>ANAQ-Sup is managed by an Executive Secretary appointed by decree on the proposal of the MESRI. The Executive Secretary holds the decision-making powers needed for ANAQ-Sup to run smoothly and ensures that the decisions of the Board of Directors and the Scientific Council are implemented. To carry out these duties, the Executive Secretary relies on a team of administrative, technical and support staff. ANAQ-Sup is a member of several international quality assurance networks, including the French-speaking African Quality Assurance Network (RAFANAQ).</p>$en$,
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
WHERE slug = 'gouvernance'
  AND author = 'migration';
