-- Site bilingue (décision ANAQ-Sup du 13/10/2026) : chaque contenu peut recevoir une version anglaise.
-- La traduction est préremplie par DeepL dans le back-office puis relue par l'éditeur avant publication.
-- Sans version anglaise, l'API renvoie la version française (champ `lang` = 'fr').
--
-- Colonnes `*_en` plutôt qu'une table de traductions : deux langues seulement, lectures simples,
-- et une contrainte garantit qu'une version anglaise est complète (titre ET corps), jamais à moitié.

ALTER TABLE news_category
    ADD COLUMN label_en text;

ALTER TABLE news_item
    ADD COLUMN title_en     text,
    ADD COLUMN summary_en   text,
    ADD COLUMN body_html_en text,
    ADD CONSTRAINT news_item_en_complete_chk CHECK ((title_en IS NULL) = (body_html_en IS NULL));

ALTER TABLE event
    ADD COLUMN title_en     text,
    ADD COLUMN summary_en   text,
    ADD COLUMN body_html_en text,
    ADD CONSTRAINT event_en_complete_chk CHECK ((title_en IS NULL) = (body_html_en IS NULL));

ALTER TABLE publication
    ADD COLUMN title_en   text,
    ADD COLUMN summary_en text;

ALTER TABLE institutional_page
    ADD COLUMN title_en     text,
    ADD COLUMN summary_en   text,
    ADD COLUMN body_html_en text,
    ADD CONSTRAINT institutional_page_en_complete_chk CHECK ((title_en IS NULL) = (body_html_en IS NULL));
