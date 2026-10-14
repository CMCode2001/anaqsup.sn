-- « À la une » (décision ANAQ-Sup du 13/10/2026) : l'article épinglé par l'éditeur, sinon le plus récent.
-- Un seul article épinglé à la fois, garanti par la base : épingler un article désépingle le précédent
-- (dans la même transaction, côté back-office).
ALTER TABLE news_item
    ADD COLUMN featured boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX news_item_single_featured_idx ON news_item ((true)) WHERE featured;

-- Liste publique : l'article épinglé d'abord, puis les plus récents.
DROP INDEX news_item_published_idx;
CREATE INDEX news_item_published_idx ON news_item (featured DESC, published_at DESC) WHERE status = 'PUBLIE';
