UPDATE photo
SET url = REGEXP_REPLACE(url, '^https?://[^/]+/[^/]+/', '')
WHERE url LIKE 'http%';

UPDATE photo
SET thumbnail_url = REGEXP_REPLACE(thumbnail_url, '^https?://[^/]+/[^/]+/', '')
WHERE thumbnail_url LIKE 'http%';

UPDATE photo
SET medium_url = REGEXP_REPLACE(medium_url, '^https?://[^/]+/[^/]+/', '')
WHERE medium_url LIKE 'http%';
