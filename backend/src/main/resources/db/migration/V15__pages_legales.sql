-- Pages légales (décision ANAQ-Sup du 14/10/2026) : « Mentions légales » et « Données personnelles et cookies »,
-- pages éditables dans le back-office (Pages institutionnelles), liées depuis le pied de page.
-- Texte pré-rempli avec ce que fait réellement le site ; ce que seule l'ANAQ-Sup peut fournir ou valider est
-- signalé en gras entre crochets : [À compléter : …] / [À valider : …].

INSERT INTO institutional_page (slug, title, summary, body_html, title_en, summary_en, body_html_en, status, author, published_at)
VALUES (
'mentions-legales',
'Mentions légales',
'Informations légales sur le site anaqsup.sn : éditeur, hébergement, contenus, propriété intellectuelle et responsabilités.',
$fr$<h2>Éditeur du site</h2>
<p>Le site anaqsup.sn est édité par l'Autorité nationale d'Assurance Qualité de l'Enseignement supérieur, de la Recherche et de l'Innovation (ANAQ-Sup), qui relève du Ministère de l'Enseignement supérieur, de la Recherche et de l'Innovation. <strong>[À compléter : statut juridique et texte de création de l'ANAQ-Sup.]</strong></p>
<ul><li>Adresse : Résidence Adja Aby Gueye, Sacré-Cœur 3 Pyrotechnie, Lot N° R126, Dakar, Sénégal</li><li>Téléphone : +221 33 825 75 18</li><li>Courriel : <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a></li></ul>
<h2>Directeur de la publication</h2>
<p><strong>[À compléter : nom et fonction du directeur de la publication, en principe le Secrétaire exécutif de l'ANAQ-Sup.]</strong></p>
<h2>Hébergement</h2>
<p>Le site et ses données sont hébergés par : <strong>[À compléter : nom, adresse et téléphone de l'hébergeur, et pays où se trouvent les serveurs.]</strong></p>
<h2>Conception et réalisation</h2>
<p><strong>[À compléter : prestataire ayant conçu et réalisé le site.]</strong></p>
<h2>Contenus et avis publiés</h2>
<p>Les informations publiées sur ce site, notamment les avis d'habilitation des établissements et d'accréditation des formations, sont fournies à titre d'information. L'ANAQ-Sup veille à leur exactitude et à leur mise à jour, sans pouvoir garantir l'absence d'erreur ou d'omission. <strong>[À valider : document qui fait foi en cas de différence avec le site (décision notifiée, texte officiel…).]</strong></p>
<p>Pour signaler une erreur : <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a>.</p>
<h2>Propriété intellectuelle</h2>
<p>Les textes, photographies, documents et le logotype présents sur ce site appartiennent à l'ANAQ-Sup ou à leurs auteurs, sauf mention contraire. Le logotype de l'ANAQ-Sup ne peut en aucun cas être modifié.</p>
<p><strong>[À valider : conditions de réutilisation des contenus, par exemple une reproduction autorisée avec mention de la source, sauf pour les photographies et le logotype.]</strong></p>
<h2>Liens vers d'autres sites</h2>
<p>Ce site propose des liens vers d'autres sites (ministère, partenaires, réseaux sociaux…). L'ANAQ-Sup n'est pas responsable de leur contenu ni de leurs pratiques en matière de données personnelles.</p>
<p><strong>[À valider : conditions dans lesquelles d'autres sites peuvent faire un lien vers anaqsup.sn.]</strong></p>
<h2>Données personnelles</h2>
<p>Les traitements de données personnelles et les cookies du site sont décrits sur la page <a href="/donnees-personnelles">Données personnelles et cookies</a>.</p>
<h2>Disponibilité du site</h2>
<p>L'ANAQ-Sup s'efforce d'assurer l'accès au site en permanence. Elle peut toutefois l'interrompre, notamment pour maintenance ou en cas de problème technique.</p>
<h2>Droit applicable</h2>
<p>Les présentes mentions sont régies par le droit sénégalais. <strong>[À valider : juridiction compétente en cas de litige.]</strong></p>$fr$,
'Legal notice',
'Legal information about the anaqsup.sn website: publisher, hosting, content, intellectual property and liability.',
$en$<h2>Website publisher</h2>
<p>The anaqsup.sn website is published by the National Authority for Quality Assurance in Higher Education, Research and Innovation (ANAQ-Sup), which comes under the Ministry of Higher Education, Research and Innovation. <strong>[To be completed: legal status of ANAQ-Sup and the instrument establishing it.]</strong></p>
<ul><li>Address: Résidence Adja Aby Gueye, Sacré-Cœur 3 Pyrotechnie, Lot N° R126, Dakar, Senegal</li><li>Telephone: +221 33 825 75 18</li><li>Email: <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a></li></ul>
<h2>Publication director</h2>
<p><strong>[To be completed: name and position of the publication director, normally the Executive Secretary of ANAQ-Sup.]</strong></p>
<h2>Hosting</h2>
<p>The website and its data are hosted by: <strong>[To be completed: name, address and telephone number of the hosting provider, and the country where the servers are located.]</strong></p>
<h2>Design and development</h2>
<p><strong>[To be completed: provider that designed and built the website.]</strong></p>
<h2>Published content and opinions</h2>
<p>The information published on this website, in particular the institutional approval and programme accreditation opinions, is provided for information purposes. ANAQ-Sup takes care to keep it accurate and up to date, but cannot guarantee that it is free of errors or omissions. <strong>[To be confirmed: which document prevails in the event of a discrepancy with the website (notified decision, official text, etc.).]</strong></p>
<p>To report an error: <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a>.</p>
<h2>Intellectual property</h2>
<p>The texts, photographs, documents and logo on this website belong to ANAQ-Sup or to their authors, unless otherwise stated. The ANAQ-Sup logo may not be modified under any circumstances.</p>
<p><strong>[To be confirmed: conditions for reusing content, for example reproduction allowed with acknowledgement of the source, except for photographs and the logo.]</strong></p>
<h2>Links to other websites</h2>
<p>This website contains links to other websites (ministry, partners, social networks, etc.). ANAQ-Sup is not responsible for their content or for their personal data practices.</p>
<p><strong>[To be confirmed: conditions under which other websites may link to anaqsup.sn.]</strong></p>
<h2>Personal data</h2>
<p>The website's processing of personal data and its cookies are described on the <a href="/en/privacy">Personal data and cookies</a> page.</p>
<h2>Website availability</h2>
<p>ANAQ-Sup strives to keep the website available at all times. It may, however, interrupt access, in particular for maintenance or in the event of a technical problem.</p>
<h2>Applicable law</h2>
<p>This legal notice is governed by Senegalese law. <strong>[To be confirmed: competent court in the event of a dispute.]</strong></p>$en$,
'PUBLIE', 'migration', now()),
(
'donnees-personnelles',
'Données personnelles et cookies',
'Quelles données le site anaqsup.sn traite, pourquoi, combien de temps, et comment exercer vos droits.',
$fr$<h2>En résumé</h2>
<ul><li>Vous pouvez consulter tout le site sans créer de compte ni donner d'information personnelle.</li><li>Le site n'utilise ni mesure d'audience, ni publicité, ni cookie de réseau social ou de service tiers. Les polices de caractères sont hébergées sur le site lui-même.</li><li>Un seul cookie peut être déposé chez les lecteurs : celui de la mention « J'aime », et seulement si vous votez.</li></ul>
<h2>Responsable du traitement</h2>
<p>ANAQ-Sup, Résidence Adja Aby Gueye, Sacré-Cœur 3 Pyrotechnie, Lot N° R126, Dakar. Contact : <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a>. <strong>[À compléter : référent pour les données personnelles, s'il est désigné.]</strong></p>
<p>Ces traitements relèvent de la loi n° 2008-12 du 25 janvier 2008 sur la protection des données à caractère personnel. <strong>[À compléter : numéro et date de la déclaration auprès de la Commission de protection des données personnelles (CDP).]</strong></p>
<h2>Lecteurs du site</h2>
<h3>Mention « J'aime » des actualités</h3>
<p>Quand vous cliquez sur « J'aime » ou « Je n'aime pas », le site dépose le cookie anaqsup_lecteur. Il contient un identifiant tiré au hasard, sans lien avec votre identité. Il sert uniquement à ne compter qu'un vote par navigateur et à vous permettre de changer d'avis. Le serveur n'enregistre qu'une empreinte de cet identifiant, avec votre vote. Le cookie expire au bout de 12 mois ; vous pouvez le supprimer à tout moment depuis votre navigateur.</p>
<h3>Journaux techniques</h3>
<p>Comme tout site web, les serveurs enregistrent automatiquement des informations sur chaque visite : adresse IP, date et heure, page demandée, navigateur utilisé. Elles servent à la sécurité du site (protection contre les attaques et les abus) et à la résolution des pannes, et ne sont pas utilisées pour suivre votre navigation. Durée de conservation : <strong>[À compléter.]</strong></p>
<h3>Réseaux sociaux et partage</h3>
<p>Les icônes Facebook, X, LinkedIn et YouTube, ainsi que les boutons de partage, sont de simples liens : aucun contenu de ces services n'est chargé sur le site tant que vous ne cliquez pas. Une fois sur leur site, leurs propres règles s'appliquent.</p>
<h2>Agents de l'ANAQ-Sup (espace de gestion)</h2>
<p>Pour les personnes qui publient sur le site, l'espace de gestion traite :</p>
<ul><li>le nom, l'adresse électronique et le rôle de chaque compte ;</li><li>le mot de passe, enregistré sous une forme chiffrée irréversible, qui n'est lisible par personne ;</li><li>la date de dernière connexion et la session en cours, close après 3 heures d'inactivité ou à la fermeture du navigateur.</li></ul>
<p>Ces données ne sont accessibles qu'aux administrateurs de l'espace de gestion. Durée de conservation après le départ d'un agent : <strong>[À compléter.]</strong></p>
<h2>Traduction automatique</h2>
<p>Pour proposer le site en anglais, les textes publiés (actualités, événements, documents, pages) sont traduits automatiquement par le service DeepL (DeepL SE, Allemagne). Seuls ces textes, déjà publics, lui sont transmis : aucune donnée concernant les lecteurs. <strong>[À valider : abonnement DeepL API et ses conditions de confidentialité.]</strong></p>
<h2>Destinataires et hébergement</h2>
<p>Les données ne sont ni vendues ni cédées. Elles ne sont accessibles qu'aux agents habilités de l'ANAQ-Sup et à son hébergeur, pour les besoins techniques du service. <strong>[À compléter : hébergeur et pays de stockage des données ; en cas de stockage hors du Sénégal, garanties prévues.]</strong></p>
<h2>Sécurité</h2>
<p>Le site n'est accessible qu'en connexion chiffrée (HTTPS). L'espace de gestion est protégé par mot de passe, avec des sessions limitées dans le temps et une protection contre les tentatives de connexion répétées.</p>
<h2>Vos droits</h2>
<p>Conformément à la loi n° 2008-12, vous disposez d'un droit d'accès, de rectification, d'opposition et de suppression des données qui vous concernent. Pour l'exercer, écrivez à <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a> en précisant votre demande. Vous pouvez aussi saisir la Commission de protection des données personnelles : <a href="https://www.cdp.sn">www.cdp.sn</a>.</p>
<h2>Cookies</h2>
<ul><li><strong>anaqsup_lecteur</strong> (lecteurs) : déposé seulement lors d'un vote « J'aime » ou « Je n'aime pas », pour ne compter qu'un vote par navigateur ; 12 mois.</li><li><strong>ANAQSUP_SESSION</strong> (agents de l'espace de gestion) : maintient la connexion ; supprimé à la fermeture du navigateur, expire après 3 heures d'inactivité.</li><li><strong>XSRF-TOKEN</strong> (agents de l'espace de gestion) : protège les actions contre les requêtes frauduleuses ; durée de la session.</li></ul>
<p>Ces cookies sont nécessaires au service que vous utilisez ; aucun ne sert à vous suivre ni à faire de la publicité. <strong>[À valider : absence de demande de consentement pour ces cookies.]</strong></p>$fr$,
'Personal data and cookies',
'What data the anaqsup.sn website processes, why, for how long, and how to exercise your rights.',
$en$<h2>In brief</h2>
<ul><li>You can browse the whole website without creating an account or providing any personal information.</li><li>The website uses no audience measurement, no advertising and no social network or third-party cookies. Fonts are hosted on the website itself.</li><li>Only one cookie may be set for readers: the "Like" cookie, and only if you vote.</li></ul>
<h2>Data controller</h2>
<p>ANAQ-Sup, Résidence Adja Aby Gueye, Sacré-Cœur 3 Pyrotechnie, Lot N° R126, Dakar. Contact: <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a>. <strong>[To be completed: personal data contact person, if one has been appointed.]</strong></p>
<p>This processing is governed by Law No. 2008-12 of 25 January 2008 on the protection of personal data. <strong>[To be completed: number and date of the declaration to the Personal Data Protection Commission (CDP).]</strong></p>
<h2>Website readers</h2>
<h3>"Like" on news articles</h3>
<p>When you click "Like" or "Dislike", the website sets the anaqsup_lecteur cookie. It contains a randomly generated identifier that is not linked to your identity. It is used only to count one vote per browser and to let you change your mind. The server stores only a fingerprint of this identifier, together with your vote. The cookie expires after 12 months; you can delete it at any time in your browser.</p>
<h3>Technical logs</h3>
<p>Like any website, the servers automatically record information about each visit: IP address, date and time, page requested and browser used. This information is used for the security of the website (protection against attacks and abuse) and for troubleshooting, and is not used to track your browsing. Retention period: <strong>[To be completed.]</strong></p>
<h3>Social networks and sharing</h3>
<p>The Facebook, X, LinkedIn and YouTube icons and the share buttons are plain links: no content from these services is loaded on the website unless you click. Once on their website, their own rules apply.</p>
<h2>ANAQ-Sup staff (management area)</h2>
<p>For the people who publish on the website, the management area processes:</p>
<ul><li>the name, email address and role of each account;</li><li>the password, stored in an irreversibly encrypted form that nobody can read;</li><li>the date of last login and the current session, which ends after 3 hours of inactivity or when the browser is closed.</li></ul>
<p>This data is accessible only to the administrators of the management area. Retention period after a staff member leaves: <strong>[To be completed.]</strong></p>
<h2>Automatic translation</h2>
<p>To offer the website in English, published texts (news, events, documents, pages) are automatically translated by the DeepL service (DeepL SE, Germany). Only these texts, which are already public, are sent to it: no data about readers. <strong>[To be confirmed: DeepL API subscription and its confidentiality terms.]</strong></p>
<h2>Recipients and hosting</h2>
<p>The data is neither sold nor transferred. It is accessible only to authorised ANAQ-Sup staff and to its hosting provider, for the technical needs of the service. <strong>[To be completed: hosting provider and country where the data is stored; if it is stored outside Senegal, the safeguards in place.]</strong></p>
<h2>Security</h2>
<p>The website is accessible only over an encrypted connection (HTTPS). The management area is password-protected, with time-limited sessions and protection against repeated login attempts.</p>
<h2>Your rights</h2>
<p>In accordance with Law No. 2008-12, you have the right to access, rectify, object to and delete data concerning you. To exercise these rights, write to <a href="mailto:secretariat@anaqsup.sn">secretariat@anaqsup.sn</a>, stating your request. You may also contact the Personal Data Protection Commission: <a href="https://www.cdp.sn">www.cdp.sn</a>.</p>
<h2>Cookies</h2>
<ul><li><strong>anaqsup_lecteur</strong> (readers): set only when you vote "Like" or "Dislike", to count one vote per browser; 12 months.</li><li><strong>ANAQSUP_SESSION</strong> (management area staff): keeps you logged in; deleted when the browser is closed, expires after 3 hours of inactivity.</li><li><strong>XSRF-TOKEN</strong> (management area staff): protects actions against fraudulent requests; lasts for the session.</li></ul>
<p>These cookies are necessary for the service you are using; none of them is used to track you or for advertising. <strong>[To be confirmed: no consent request for these cookies.]</strong></p>$en$,
'PUBLIE', 'migration', now())
ON CONFLICT (slug) DO NOTHING;

-- Version anglaise rédigée avec la version française : traitée comme une traduction automatique à jour.
-- Si l'ANAQ-Sup modifie le français, DeepL la refait ; si elle corrige l'anglais, elle devient définitive.
UPDATE institutional_page
SET en_auto   = true,
    en_source = md5(concat_ws(E'\x1f', title, coalesce(summary, ''), body_html, coalesce(image_role, '')))
WHERE slug IN ('mentions-legales', 'donnees-personnelles')
  AND author = 'migration';
