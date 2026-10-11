-- Dados fictícios para treino. Os números foram escolhidos para que:
--   PRESIDENTE vá para o 2º turno (líder com ~38% dos válidos),
--   GOVERNADOR seja decidido no 1º turno (~59%),
--   SENADOR seja decidido por maioria simples,
--   a seção 2 da zona 3 ainda não tenha enviado boletim (apuração parcial).

INSERT INTO eleicao (descricao, ano, turno) VALUES ('Eleição Geral 2026 - 1º turno', 2026, 1);

INSERT INTO secao (zona, numero, municipio, uf, eleitores_aptos) VALUES
    (1, 1, 'Brasília', 'DF', 350),
    (1, 2, 'Brasília', 'DF', 320),
    (1, 3, 'Brasília', 'DF', 400),
    (2, 1, 'Brasília', 'DF', 380),
    (2, 2, 'Brasília', 'DF', 300),
    (2, 3, 'Brasília', 'DF', 360),
    (3, 1, 'Brasília', 'DF', 310),
    (3, 2, 'Brasília', 'DF', 330);

INSERT INTO candidato (eleicao_id, cargo, numero, nome, partido)
SELECT e.id, c.cargo, c.numero, c.nome, c.partido
FROM eleicao e
CROSS JOIN (VALUES
    ('PRESIDENTE', '10',  'Ana Ribeiro',     'PAB'),
    ('PRESIDENTE', '20',  'Bruno Tavares',   'PCD'),
    ('PRESIDENTE', '30',  'Carla Menezes',   'PEF'),
    ('PRESIDENTE', '40',  'Diego Arantes',   'PGH'),
    ('PRESIDENTE', '50',  'Elias Rocha',     'PIJ'),
    ('GOVERNADOR', '10',  'Fernanda Lopes',  'PAB'),
    ('GOVERNADOR', '20',  'Gustavo Prado',   'PCD'),
    ('GOVERNADOR', '30',  'Helena Duarte',   'PEF'),
    ('SENADOR',    '101', 'Igor Santana',    'PAB'),
    ('SENADOR',    '202', 'Júlia Fontes',    'PCD'),
    ('SENADOR',    '404', 'Kátia Moreira',   'PGH')
) AS c (cargo, numero, nome, partido)
WHERE e.ano = 2026 AND e.turno = 1;

INSERT INTO boletim_urna (eleicao_id, secao_id, comparecimento, recebido_em)
SELECT e.id, s.id, b.comparecimento, TIMESTAMP WITH TIME ZONE '2026-10-04 17:30:00-03:00'
FROM eleicao e
CROSS JOIN (VALUES
    (1, 1, 300),
    (1, 2, 280),
    (1, 3, 250),
    (2, 1, 340),
    (2, 2, 200),
    (2, 3, 330),
    (3, 1, 290)
) AS b (zona, secao, comparecimento)
JOIN secao s ON s.zona = b.zona AND s.numero = b.secao
WHERE e.ano = 2026 AND e.turno = 1;

-- Brancos e nulos são iguais para os três cargos em cada seção.
INSERT INTO boletim_cargo (boletim_id, cargo, brancos, nulos)
SELECT bu.id, cg.cargo, bn.brancos, bn.nulos
FROM (VALUES
    (1, 1, 10, 15),
    (1, 2, 10, 15),
    (1, 3, 12, 18),
    (2, 1, 10, 15),
    (2, 2, 10, 15),
    (2, 3, 12, 18),
    (3, 1, 12, 18)
) AS bn (zona, secao, brancos, nulos)
CROSS JOIN (VALUES ('PRESIDENTE'), ('GOVERNADOR'), ('SENADOR')) AS cg (cargo)
JOIN secao s ON s.zona = bn.zona AND s.numero = bn.secao
JOIN boletim_urna bu ON bu.secao_id = s.id;

INSERT INTO voto_candidato (boletim_id, candidato_id, votos)
SELECT bu.id, c.id, v.votos
FROM (VALUES
    (1, 1, 'PRESIDENTE', '10', 110), (1, 1, 'PRESIDENTE', '20',  95), (1, 1, 'PRESIDENTE', '30', 50), (1, 1, 'PRESIDENTE', '40', 20),
    (1, 2, 'PRESIDENTE', '10', 100), (1, 2, 'PRESIDENTE', '20',  90), (1, 2, 'PRESIDENTE', '30', 45), (1, 2, 'PRESIDENTE', '40', 20),
    (1, 3, 'PRESIDENTE', '10',  80), (1, 3, 'PRESIDENTE', '20',  85), (1, 3, 'PRESIDENTE', '30', 40), (1, 3, 'PRESIDENTE', '40', 15),
    (2, 1, 'PRESIDENTE', '10', 120), (2, 1, 'PRESIDENTE', '20', 110), (2, 1, 'PRESIDENTE', '30', 60), (2, 1, 'PRESIDENTE', '40', 25),
    (2, 2, 'PRESIDENTE', '10',  60), (2, 2, 'PRESIDENTE', '20',  75), (2, 2, 'PRESIDENTE', '30', 30), (2, 2, 'PRESIDENTE', '40', 10),
    (2, 3, 'PRESIDENTE', '10', 115), (2, 3, 'PRESIDENTE', '20', 105), (2, 3, 'PRESIDENTE', '30', 55), (2, 3, 'PRESIDENTE', '40', 25),
    (3, 1, 'PRESIDENTE', '10', 105), (3, 1, 'PRESIDENTE', '20',  85), (3, 1, 'PRESIDENTE', '30', 50), (3, 1, 'PRESIDENTE', '40', 20),

    (1, 1, 'GOVERNADOR', '10', 170), (1, 1, 'GOVERNADOR', '20',  80), (1, 1, 'GOVERNADOR', '30', 25),
    (1, 2, 'GOVERNADOR', '10', 160), (1, 2, 'GOVERNADOR', '20',  70), (1, 2, 'GOVERNADOR', '30', 25),
    (1, 3, 'GOVERNADOR', '10', 130), (1, 3, 'GOVERNADOR', '20',  65), (1, 3, 'GOVERNADOR', '30', 25),
    (2, 1, 'GOVERNADOR', '10', 190), (2, 1, 'GOVERNADOR', '20',  95), (2, 1, 'GOVERNADOR', '30', 30),
    (2, 2, 'GOVERNADOR', '10',  95), (2, 2, 'GOVERNADOR', '20',  60), (2, 2, 'GOVERNADOR', '30', 20),
    (2, 3, 'GOVERNADOR', '10', 175), (2, 3, 'GOVERNADOR', '20',  95), (2, 3, 'GOVERNADOR', '30', 30),
    (3, 1, 'GOVERNADOR', '10', 150), (3, 1, 'GOVERNADOR', '20',  85), (3, 1, 'GOVERNADOR', '30', 25),

    (1, 1, 'SENADOR', '101', 120), (1, 1, 'SENADOR', '202', 100), (1, 1, 'SENADOR', '404', 55),
    (1, 2, 'SENADOR', '101',  95), (1, 2, 'SENADOR', '202', 105), (1, 2, 'SENADOR', '404', 55),
    (1, 3, 'SENADOR', '101',  70), (1, 3, 'SENADOR', '202', 100), (1, 3, 'SENADOR', '404', 50),
    (2, 1, 'SENADOR', '101', 125), (2, 1, 'SENADOR', '202', 120), (2, 1, 'SENADOR', '404', 70),
    (2, 2, 'SENADOR', '101',  60), (2, 2, 'SENADOR', '202',  80), (2, 2, 'SENADOR', '404', 35),
    (2, 3, 'SENADOR', '101', 110), (2, 3, 'SENADOR', '202', 125), (2, 3, 'SENADOR', '404', 65),
    (3, 1, 'SENADOR', '101', 100), (3, 1, 'SENADOR', '202',  95), (3, 1, 'SENADOR', '404', 65)
) AS v (zona, secao, cargo, numero, votos)
JOIN secao s ON s.zona = v.zona AND s.numero = v.secao
JOIN boletim_urna bu ON bu.secao_id = s.id
JOIN candidato c ON c.eleicao_id = bu.eleicao_id AND c.cargo = v.cargo AND c.numero = v.numero;
