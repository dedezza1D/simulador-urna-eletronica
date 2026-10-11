-- Respostas dos exercícios de sql/exercicios.sql.
-- ConsultasSqlTest executa este arquivo na ordem e confere os resultados.

-- R1: LEFT JOIN mantém candidatos sem voto. COALESCE troca NULL por 0.
-- Agrupar por c.id evita juntar dois candidatos homônimos.
SELECT c.numero, c.nome, c.partido, COALESCE(SUM(v.votos), 0) AS total
FROM candidato c
LEFT JOIN voto_candidato v ON v.candidato_id = c.id
WHERE c.cargo = 'PRESIDENTE'
GROUP BY c.id, c.numero, c.nome, c.partido
ORDER BY total DESC;

-- R2: WHERE filtra linhas antes de agrupar. HAVING filtra grupos depois.
SELECT c.cargo, c.nome, SUM(v.votos) AS total
FROM candidato c
JOIN voto_candidato v ON v.candidato_id = c.id
GROUP BY c.id, c.cargo, c.nome
HAVING SUM(v.votos) > 600
ORDER BY total DESC;

-- R3: 100.0 força divisão decimal. O alias pct não pode ser usado no HAVING
-- (no padrão SQL e no Oracle), por isso a expressão se repete.
SELECT s.zona,
       SUM(s.eleitores_aptos) AS aptos,
       SUM(b.comparecimento) AS comparecimento,
       ROUND(100.0 * SUM(b.comparecimento) / SUM(s.eleitores_aptos), 2) AS pct
FROM secao s
JOIN boletim_urna b ON b.secao_id = s.id
GROUP BY s.zona
HAVING 100.0 * SUM(b.comparecimento) / SUM(s.eleitores_aptos) < 80
ORDER BY s.zona;

-- R4a: NOT EXISTS. Costuma ser a forma mais clara e o otimizador resolve bem.
SELECT s.zona, s.numero
FROM secao s
WHERE NOT EXISTS (SELECT 1 FROM boletim_urna b WHERE b.secao_id = s.id)
ORDER BY s.zona, s.numero;

-- R4b: LEFT JOIN + IS NULL. Mesmo resultado. Evite NOT IN com subconsulta
-- que pode retornar NULL: se houver um NULL, NOT IN não retorna nada.
SELECT s.zona, s.numero
FROM secao s
LEFT JOIN boletim_urna b ON b.secao_id = s.id
WHERE b.id IS NULL
ORDER BY s.zona, s.numero;

-- R5
SELECT s.zona, s.numero, b.comparecimento,
       bc.brancos + bc.nulos AS brancos_nulos,
       ROUND(100.0 * (bc.brancos + bc.nulos) / b.comparecimento, 2) AS pct
FROM boletim_cargo bc
JOIN boletim_urna b ON b.id = bc.boletim_id
JOIN secao s ON s.id = b.secao_id
WHERE bc.cargo = 'PRESIDENTE'
  AND 100.0 * (bc.brancos + bc.nulos) / b.comparecimento > 10
ORDER BY pct DESC;

-- R6: a janela é calculada depois do GROUP BY. Para filtrar pela posição
-- é preciso uma subconsulta (ou CTE), porque WHERE roda antes da janela.
-- RANK repete a posição em empates. ROW_NUMBER escolheria um só.
SELECT cargo, nome, total
FROM (
    SELECT c.cargo, c.nome, COALESCE(SUM(v.votos), 0) AS total,
           RANK() OVER (PARTITION BY c.cargo ORDER BY COALESCE(SUM(v.votos), 0) DESC) AS posicao
    FROM candidato c
    LEFT JOIN voto_candidato v ON v.candidato_id = c.id
    GROUP BY c.id, c.cargo, c.nome
) ranking
WHERE posicao = 1
ORDER BY cargo;

-- R7: COUNT(DISTINCT c.id) conta candidatos, não linhas de voto.
SELECT c.partido, COUNT(DISTINCT c.id) AS candidatos, COALESCE(SUM(v.votos), 0) AS total
FROM candidato c
LEFT JOIN voto_candidato v ON v.candidato_id = c.id
GROUP BY c.partido
HAVING COUNT(DISTINCT c.id) >= 2
ORDER BY total DESC;

-- R8
SELECT s.zona, s.numero, b.comparecimento
FROM boletim_urna b
JOIN secao s ON s.id = b.secao_id
ORDER BY b.comparecimento DESC
FETCH FIRST 3 ROWS ONLY;
