-- =====================================================================
-- Exercícios de SQL sobre o banco do projeto (H2, sintaxe compatível com
-- Oracle 12c+ e PostgreSQL).
--
-- Como rodar: suba a aplicação e abra http://localhost:8081/h2-console
--   JDBC URL: jdbc:h2:mem:votacao   Usuário: sa   Senha: (vazia)
--
-- Tabelas:
--   eleicao(id, descricao, ano, turno)
--   secao(id, zona, numero, municipio, uf, eleitores_aptos)
--   candidato(id, eleicao_id, cargo, numero, nome, partido)
--   boletim_urna(id, eleicao_id, secao_id, comparecimento, recebido_em)
--   boletim_cargo(id, boletim_id, cargo, brancos, nulos)
--   voto_candidato(id, boletim_id, candidato_id, votos)
--
-- Há uma única eleição nos dados, então não é preciso filtrar por eleicao_id.
-- Respostas em sql/respostas.sql (tente antes de olhar). Tempo-alvo: 5 min cada.
-- =====================================================================

-- E1 (LEFT JOIN + GROUP BY)
-- Total de votos de cada candidato a PRESIDENTE: número, nome, partido e total,
-- do mais votado para o menos votado. Candidatos sem voto devem aparecer com 0.
-- Esperado: 5 linhas, começando por Ana Ribeiro (690) e terminando em Elias Rocha (0).


-- E2 (HAVING)
-- Candidatos de qualquer cargo com mais de 600 votos no total.
-- Mostre cargo, nome e total, do maior para o menor.
-- Esperado: 5 linhas, começando por Fernanda Lopes (1070).


-- E3 (GROUP BY + HAVING com expressão)
-- Comparecimento por zona, considerando só seções que já enviaram boletim:
-- zona, soma de eleitores aptos, soma do comparecimento e percentual (2 casas).
-- Mostre apenas zonas com comparecimento abaixo de 80%.
-- Esperado: 1 linha, zona 1, 1070 aptos, 830 comparecimento, 77.57%.


-- E4 (anti-join)
-- Seções que ainda não enviaram boletim de urna (zona e número).
-- Faça de duas formas: NOT EXISTS e LEFT JOIN ... IS NULL.
-- Esperado: zona 3, seção 2.


-- E5 (JOIN de 3 tabelas + cuidado com divisão inteira)
-- Seções em que brancos + nulos para PRESIDENTE passaram de 10% do comparecimento.
-- Mostre zona, seção, comparecimento, brancos + nulos e o percentual (2 casas),
-- do maior percentual para o menor.
-- Esperado: 3 linhas: z2 s2 (12.50), z1 s3 (12.00), z3 s1 (10.34).
-- Pegadinha: 25 * 100 / 300 em inteiros dá 8, não 8.33.


-- E6 (função de janela)
-- O primeiro colocado de cada cargo, usando RANK() OVER (PARTITION BY ...).
-- Mostre cargo, nome e total, ordenado por cargo.
-- Esperado: GOVERNADOR Fernanda Lopes 1070, PRESIDENTE Ana Ribeiro 690, SENADOR Júlia Fontes 725.


-- E7 (HAVING com COUNT DISTINCT)
-- Total de votos por partido, somando todos os cargos, apenas para partidos
-- com pelo menos 2 candidatos. Mostre partido, quantidade de candidatos e total.
-- Esperado: PAB 3 2440, PCD 3 1920, PGH 2 530, PEF 2 510.
-- Pegadinha: o JOIN com voto_candidato multiplica as linhas; COUNT(*) dá errado.


-- E8 (paginação no padrão SQL)
-- As 3 seções com maior comparecimento (zona, seção, comparecimento).
-- Use FETCH FIRST (Oracle 12c+, PostgreSQL, H2), não ROWNUM nem LIMIT.
-- Esperado: z2 s1 340, z2 s3 330, z1 s1 300.
