UPDATE users
SET phone = '+' || phone
WHERE phone IS NOT NULL
  AND phone NOT LIKE '+%'
  AND phone NOT LIKE '00%';
