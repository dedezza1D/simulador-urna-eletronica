# Live coding: exercícios e perguntas prováveis

## Como treinar

- **Cronometre.** Numa entrevista de 60 minutos, o exercício costuma ter de 20 a 40.
- **Fale enquanto faz:** o que vai escrever, por quê, e o que deixaria para depois. Narrar o raciocínio pesa tanto quanto o código.
- Comece pelo caso feliz, rode, e só depois trate erros e casos de borda.
- Ao terminar, rode `./mvnw test` e diga o que testaria a mais.

## Exercício 1: CRUD do zero (meta: 25 min)

Crie o recurso **Partido** (`numero` de 2 dígitos, `sigla` de até 10 caracteres, `nome`), com `GET` paginado, `GET /{id}`, `POST`, `PUT` e `DELETE`. Faça sem olhar `candidato/` e compare só no final.

Checklist:
- [ ] Migration Flyway `V5__partido.sql` com `UNIQUE (numero)` e `UNIQUE (sigla)`
- [ ] Entidade com construtor protegido para o JPA e sem setters públicos desnecessários
- [ ] DTOs `record` de entrada e saída (a entidade não sai pela API)
- [ ] Bean Validation no request e `@Valid` no controller
- [ ] `201 Created` com `Location`; `204` no delete; `404` para inexistente
- [ ] Duplicidade → `409` (verificação no serviço **e** constraint no banco)
- [ ] Um teste `@WebMvcTest` (400 de validação) e um teste de serviço com Mockito

## Exercício 2: teste unitário com Mockito (meta: 15 min)

`ResultadoService` não tem teste de unidade. Escreva `ResultadoServiceTest` com `@ExtendWith(MockitoExtension.class)`:
- eleição inexistente → `RecursoNaoEncontradoException`, e nenhuma consulta de apuração é feita (`verify(..., never())`);
- 7 de 8 seções → `parcial = true` e `percentualSecoesTotalizadas = 87.50`;
- 0 seções cadastradas → percentual `0.00` sem divisão por zero.

## Exercício 3: TDD de uma regra nova (meta: 20 min)

Regra: *o BU deve conter todos os cargos que têm candidatos na eleição.* Hoje um BU só com PRESIDENTE é aceito.
1. Escreva primeiro o teste em `BoletimServiceTest` (vermelho).
2. Implemente o mínimo em `BoletimService` (verde).
3. Refatore. Repare que `VotacaoIntegracaoTest` continua passando, porque os BUs de lá têm os três cargos.

## Exercício 4: SQL (meta: 5 min cada)

[`sql/exercicios.sql`](../sql/exercicios.sql). Rode no H2 console. `ConsultasSqlTest` confere as respostas.

## Exercício 5: refatoração (meta: 30 min)

[`03-refatoracao.md`](03-refatoracao.md).

## Exercício 6: frontend (meta: 20 min)

Hoje a urna (`static/urna.js`) descobre a habilitação consultando o servidor a cada segundo (polling). Troque por **Server-Sent Events**:
- backend: um endpoint `GET …/urna/eventos` com `SseEmitter`, que avisa quando a urna é habilitada, cancelada ou encerrada;
- frontend: `new EventSource(...)` no lugar do `setInterval`, com reconexão automática.

Discuta: polling × SSE × WebSocket para esse caso. Pense em quantos terminais há por seção, em proxies e balanceadores, e no que acontece quando a conexão cai no meio de um voto.

---

## Perguntas prováveis sobre este código

**Por que `spring.jpa.open-in-view: false`?**
Com ele ligado, a sessão do Hibernate fica aberta até a resposta ser renderizada, e um acesso lazy no controller dispara SQL fora da transação. Isso esconde problemas N+1 e segura a conexão por mais tempo. Desligado, o lazy loading fora do serviço falha cedo e fica visível.

**Por que `ddl-auto: validate` com Flyway?**
O schema é versionado em SQL revisável (migrations). O Hibernate só confere se as entidades batem com o banco e nunca altera tabela em produção.

**Em `CandidatoService.atualizar` não há `save`. Por quê?**
A entidade foi carregada dentro da transação e está gerenciada. O *dirty checking* detecta a alteração e gera o `UPDATE` no flush/commit.

**Onde o código evita N+1?**
- `BoletimService` carrega todos os candidatos da eleição numa consulta e monta um `Map`, em vez de buscar candidato por voto.
- `ApuracaoRepository` agrega no banco (`SUM`, `GROUP BY`) e devolve projeções `record`, sem carregar entidades.

**`existsBy...` e depois `save`: isso tem condição de corrida?**
Tem. Duas requisições podem passar pelo `exists` juntas. Por isso há `UNIQUE` no banco, e a `DataIntegrityViolationException` vira 409 no `TratadorDeErros`. A verificação no serviço dá a mensagem amigável; a constraint dá a garantia.

**E duas habilitações simultâneas na mesma urna?**
`@Version` em `Urna` (lock otimista). A segunda transação falha no commit com `ObjectOptimisticLockingFailureException`, que vira 409.

**400, 404, 409 ou 422?**
- **400**: requisição malformada ou inválida no formato (Bean Validation, JSON quebrado, enum inexistente).
- **404**: recurso não existe.
- **409**: conflita com o estado atual (duplicado, já votou, urna ocupada).
- **422**: bem formado, mas viola regra de negócio (soma de votos ≠ comparecimento, assinatura inválida).

**`registrar(request)` chama `registrar(request, null)`, e os dois têm `@Transactional`. Funciona?**
A chamada interna não passa pelo proxy, então a anotação do segundo método é ignorada nessa chamada (*self-invocation*). Aqui não importa, porque o primeiro já abriu a transação. Seria um bug se o método interno precisasse de `REQUIRES_NEW` ou `readOnly` diferente.

**Por que `Clock` injetado?**
O horário vira dependência: o teste usa `Clock.fixed` e verifica `recebidoEm` com exatidão.

**Por que `votosDoLider * 2 > validos` e não `percentual > 50`?**
Inteiros não têm erro de arredondamento, e o operador `>` deixa explícito que exatamente 50% não elege.

**Por que lista branca na ordenação?**
`sort` vem do cliente e vira `ORDER BY`. Sem a lista, um campo inexistente vira 500, e o cliente pode ordenar por colunas que não deveria ver.

**Como o RDV preserva o sigilo do voto?**
A tabela não tem id sequencial, horário nem referência ao eleitor. Cada voto vai para uma posição sorteada (`SecureRandom`) e cada cargo é sorteado independentemente. A listagem sempre usa `ORDER BY posicao`, para a ordem física de inserção não vazar. O comparecimento (quem votou) fica em outra tabela, sem horário.

**Por que a assinatura é verificada antes de ler o JSON?**
Nada do conteúdo é confiável antes de provar a origem. A chave pública vem do cadastro da urna, nunca do envelope. Se viesse do envelope, um atacante mandaria a própria chave junto.

**Por que a retransmissão responde 200 e não 409?**
Em rede instável, o ponto de transmissão reenvia sem saber se o primeiro envio chegou. Mesmo hash → mesma operação → sucesso sem duplicar (idempotência). Hash diferente para a mesma seção é anomalia → 409.

**A chave privada fica em memória. Em produção?**
Num HSM ou no hardware de segurança da urna, sem nunca sair dele. Aqui `CofreDeChaves` simula esse isolamento: a chave não vai para o banco nem para nenhuma resposta da API.
