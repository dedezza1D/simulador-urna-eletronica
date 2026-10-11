-- 330 eleitores fictícios para a seção 2 da zona 3, a única sem boletim nos dados de exemplo.
-- Títulos de 900000000001 a 900000000330. SYSTEM_RANGE é específico do H2.
INSERT INTO eleitor (titulo, nome, secao_id)
SELECT '9' || LPAD(CAST(r.X AS VARCHAR), 11, '0'),
       'Eleitor ' || LPAD(CAST(r.X AS VARCHAR), 3, '0'),
       s.id
FROM SYSTEM_RANGE(1, 330) r
JOIN secao s ON s.zona = 3 AND s.numero = 2;
