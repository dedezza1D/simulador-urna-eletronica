# Como funciona o sistema eleitoral brasileiro

Resumo técnico: o caminho do voto, as auditorias, as regras de apuração e onde cada conceito aparece neste projeto. Fontes ao final.

## 1. Linha do tempo de uma eleição

| Fase | O que acontece | Sistemas citados |
|---|---|---|
| Cadastro eleitoral | Alistamento, transferência, biometria. Base para as listas de eleitores de cada seção. | Cadastro Eleitoral |
| Registro de candidaturas | Partidos montam os pedidos (individuais, coletivos, DRAP) e enviam à Justiça Eleitoral. Há integração com Receita Federal, filiação partidária e PJe. | CANDex, DivulgaCandContas |
| Preparação dos programas | Os programas da urna e dos sistemas centrais são compilados, **assinados digitalmente e lacrados** em cerimônia pública com OAB, MPF e outras entidades. Os hashes são publicados. Em 2026: 4 de setembro. | — |
| Carga das urnas | Cada urna recebe os programas lacrados e os dados da sua seção. | — |
| Véspera / manhã da eleição | **Zerésima**: a urna imprime um relatório provando que não há nenhum voto registrado. Em 2026 o TSE também emitiu zerésima dos sistemas centrais. | — |
| Votação | Identificação do eleitor (biometria quando disponível) e voto. A urna **não tem conexão** com internet, Wi-Fi ou Bluetooth. | Urna eletrônica |
| Encerramento | A urna gera o **RDV** e, a partir dele, o **BU**. O BU é impresso em várias vias (uma é afixada na porta da seção) e gravado, criptografado e assinado, na **mídia de resultado**. | — |
| Transmissão | A mídia segue para o cartório (Sistema Transportador) ou é transmitida do próprio local de votação pelo **JE-Connect**, por rede privada ou satélite. | Transportador, JE-Connect |
| Recepção e totalização | O sistema central **verifica a assinatura digital** (BU sem assinatura válida é descartado) e a **consistência** (ex.: votos x comparecimento) e soma os votos. A totalização é centralizada no TSE. | Totalização |
| Divulgação e auditoria | Resultados por seção no Portal de Resultados e no app Boletim na Mão. BUs e RDVs ficam disponíveis para partidos e para o público, permitindo recontagem independente. | Resultados, Dados Abertos |
| Depois da eleição | Prestação de contas de campanha e diplomação. | SPCE, DivulgaCandContas |

Dados de 2026: 158.745.463 eleitores aptos, 5.571 municípios e 186 cidades no exterior. Só São Paulo teve 103.904 seções e cerca de 114,5 mil urnas preparadas, contando as de contingência. 1º turno em 4/10/2026 e 2º turno em 25/10/2026.

## 2. Artefatos que você precisa saber explicar

- **RDV (Registro Digital do Voto):** tabela digital dos votos, gravada em **ordem aleatória** para que não seja possível ligar um voto a um eleitor. Substituiu o voto impresso em 2003. É a partir dele que o BU é gerado, e ele permite recontar os votos.
- **BU (Boletim de Urna):** resultado da seção. Traz votos por candidato e legenda, brancos, nulos e comparecimento. É impresso e também gravado com assinatura digital.
- **Zerésima:** prova de que a urna começou zerada.
- **Mídia de resultado:** dispositivo removível em formato próprio da Justiça Eleitoral, com o BU criptografado e assinado.
- **Hash / resumo digital:** a "impressão digital" de cada programa. Fiscais comparam o hash do que está rodando com o hash publicado na lacração.

## 3. Camadas de auditoria

1. **Teste Público de Segurança (TPS):** no ano anterior à eleição, investigadores externos tentam quebrar a urna e os sistemas. Depois vem o **Teste de Confirmação**, para verificar se as correções foram aplicadas.
2. **Cerimônia de assinatura e lacração** dos programas, com publicação dos hashes.
3. **Teste de Autenticidade** (dia da eleição): em urnas sorteadas, confere se os programas em execução são os oficiais.
4. **Teste de Integridade** (dia da eleição): votação paralela com cédulas de papel numa urna sorteada. Ao final, compara a contagem da urna com as cédulas. Desde 2026, voluntários podem habilitar a urna de teste com a própria biometria.
5. **Publicação de BUs e RDVs:** qualquer pessoa pode conferir o BU da porta da seção contra o publicado e recontar com o RDV.

## 4. Regras de apuração

**Votos válidos** = votos nominais + votos de legenda (na eleição proporcional). **Brancos e nulos não são válidos.**

**Majoritária** (Presidente, Governador, Senador, Prefeito):
- **Com 2º turno** (Presidente, Governador e Prefeito de município com mais de 200 mil eleitores): eleito quem tiver **mais da metade** dos votos válidos. Se ninguém tiver, os dois mais votados vão ao 2º turno. Em empate, o critério é o **mais idoso**.
- **Maioria simples** (Senador e Prefeito dos demais municípios): elege-se o mais votado.

**Proporcional** (Deputados e Vereadores):
1. **Quociente eleitoral (QE)** = votos válidos ÷ número de vagas.
2. **Quociente partidário (QP)** = votos do partido ÷ QE, desprezando a fração. É o número de vagas do partido. Para ocupar uma delas, o candidato precisa ter pelo menos **10% do QE**.
3. **Sobras:** as vagas restantes vão para as **maiores médias** (votos do partido ÷ (vagas já obtidas + 1)), recalculando a cada vaga. A Lei 14.211/2021 restringiu essa disputa a partidos com pelo menos **80% do QE** e candidatos com pelo menos **20% do QE**. O STF depois derrubou a restrição na etapa final ("sobras das sobras"). As fontes divergem sobre a partir de quando isso valeu, então confira o texto da decisão se o assunto surgir.

## 5. Caso real que vale citar: o atraso da totalização em 2020

Em 2020 a totalização passou a ser **centralizada no TSE**, por recomendação da Polícia Federal, para reduzir a superfície de ataque. Ela roda em servidores Oracle (Exadata) numa sala-cofre, operados pela equipe do tribunal. No 1º turno houve atraso na divulgação. Duas explicações foram publicadas:

- falha em um dos nós de processamento;
- o otimizador do banco escolheu planos de execução ruins porque o equipamento era novo e as estatísticas foram calculadas com a **base vazia**.

O TSE também citou atraso na entrega do equipamento por causa da pandemia, o que reduziu o tempo de preparação.

**Lição:** teste de carga só vale com volume e distribuição de dados realistas. Inclua estatísticas do banco atualizadas, planos de execução validados e um ensaio geral antes do dia.

## 6. Onde isso aparece no projeto

| Conceito real | No projeto |
|---|---|
| Zerésima | `UrnaService.abrir` → `ZeresimaResponse` (conta o RDV e prova que está vazio) |
| Mesário habilita um eleitor por vez | `Urna.habilitar` + `@Version` (lock otimista) |
| Eleitor vota uma única vez | tabela `comparecimento` com `UNIQUE (urna_id, eleitor_id)` |
| Ordem dos cargos na urna | `Cargo.ordemNaUrna`: Senador → Governador → Presidente |
| Número inexistente = voto nulo | `UrnaService.apurar` e a tela "NÚMERO ERRADO / VOTO NULO" |
| RDV em ordem aleatória, sem ligação com o eleitor | `RdvRepository`: posição sorteada por cargo, sem id, horário ou eleitor |
| BU gerado a partir do RDV | `UrnaService.encerrar` → `montarBoletim` |
| BU assinado digitalmente | Ed25519; chave privada em `CofreDeChaves`, pública cadastrada na `urna` |
| BU sem assinatura válida é descartado | `ReceptorBoletimService` (422) |
| Verificação de consistência na recepção | `BoletimService`: nominais + brancos + nulos = comparecimento; comparecimento ≤ aptos |
| Um BU por seção; retransmissão segura | `uk_boletim_secao` + `hash_conteudo` (mesmo hash → 200, outro → 409) |
| Votos válidos excluem brancos e nulos | `Apuracao.apurar` |
| Maioria absoluta / 2º turno / maioria simples | `Cargo.temSegundoTurno` + `Apuracao.situacao` |
| Totalização parcial | `ResultadoResponse.parcial` e `percentualSecoesTotalizadas` |

**Simplificações em relação ao real:** sem voto de legenda nem cargos proporcionais; sem biometria; a urna é um serviço web, não um equipamento isolado (no real ela não tem rede); a chave fica em memória, não em hardware; o voto só é gravado quando o eleitor conclui todos os cargos.

## Fontes

- TSE — [Votação segura](https://www.tse.jus.br/eleicoes/historia/processo-eleitoral-brasileiro/votacao/votacao-segura)
- TSE — [Resolução nº 23.673/2021 (procedimentos de fiscalização e auditoria)](https://www.tse.jus.br/legislacao/compilada/res/2021/resolucao-no-23-673-14-de-dezembro-de-2021)
- TSE — [Eleições 2026 em números](https://www.tse.jus.br/comunicacao/noticias/2026/Outubro/eleicoes-2026-em-numeros-158-7-milhoes-de-eleitores-estao-aptos-a-votar)
- TSE — [Registro Digital do Voto: glossário](https://www.tse.jus.br/comunicacao/noticias/2023/Abril/registro-digital-do-voto-glossario-explica-o-que-e-e-como-acessar)
- TSE — [Como funciona o Teste de Integridade](https://www.tse.jus.br/comunicacao/noticias/2026/Agosto/como-funciona-o-teste-de-integridade-a-prova-dos-nove-da-urna-eletronica)
- TSE — [Etapas de auditoria da urna](https://www.tse.jus.br/comunicacao/noticias/2024/Junho/eleicoes-2024-saiba-quais-as-etapas-de-auditoria-dos-sistemas-eleitorais-1)
- TSE — [Sistema de Candidaturas (CANDex)](https://www.tse.jus.br/eleicoes/eleicoes-2026-content/sistema-de-candidaturas-modulo-externo-candex-2026)
- TSE — [Aplicativos da Justiça Eleitoral](https://www.tse.jus.br/eleicoes/aplicativos-justica-eleitoral)
- TRE-SP — [Votos chegam seguros até a totalização (2026)](https://www.tre-sp.jus.br/comunicacao/noticias/2026/Outubro/resultados-eleicoes-2026-votos-chegam-seguros-ate-a-totalizacao)
- TRE-SP — [103 mil seções em São Paulo](https://www.tre-sp.jus.br/comunicacao/noticias/2026/Outubro/eleicoes-2026-urnas-sao-enviadas-para-as-103-mil-secoes-eleitorais-em-sao-paulo)
- TRE-SP — [Quociente eleitoral e partidário](https://www.tre-sp.jus.br/comunicacao/noticias/2026/Junho/quociente-eleitoral-e-partidario-explica-por-que-o-mais-votado-pode-nao-ser-eleito)
- TRE-MG — [Sistema proporcional de votação](https://www.tre-mg.jus.br/eleicoes/eleicoes-2022/sistema-proporcional-de-votacao)
- Senado Notícias — [Novas regras para sobras (Lei 14.211/2021)](https://www12.senado.leg.br/noticias/materias/2021/10/04/lei-preve-novas-regras-para-sobras-nas-eleicoes-de-deputados-e-vereadores)
- STF — [Regra de distribuição de sobras invalidada](https://portal.stf.jus.br/noticias/verNoticiaDetalhe.asp?idConteudo=528283&ori=1)
- Câmara dos Deputados — [O caminho do voto](https://www.camara.leg.br/noticias/903942-conheca-o-caminho-do-voto-desde-a-secao-eleitoral-ate-a-divulgacao-do-resultado)
- Aos Fatos — [Supercomputador da totalização](https://www.aosfatos.org/noticias/e-falso-que-tse-nao-controle-supercomputador-responsavel-por-somar-votos/)
- IT Forum — [Atraso na contagem em 2020](https://itforum.com.br/noticias/falha-de-ia-do-supercomputador-oracle-atrasa-contagem-de-votos-no-primeiro-turno-das-eleicoes/)
- TSE — [Ataques em 2020 não afetaram resultados](https://www.tse.jus.br/comunicacao/noticias/2020/Novembro/tentativas-de-ataques-de-hackers-ao-sistema-do-tse-nao-afetaram-resultados-das-eleicoes-afirma-barroso)
