# Simulador de urna eletrônica

Simulador de urna eletrônica e totalização em Java 21 + Spring Boot 3.5, inspirado no processo eleitoral brasileiro. É um projeto de estudo de engenharia de software web:

- API REST com validação e tratamento de erros;
- JPA/Hibernate, SQL e modelagem relacional;
- testes de unidade, de integração e de banco;
- DDD, regras de negócio e segurança (assinatura digital, idempotência, sigilo do voto);
- refatoração e frontend em HTML/CSS/JS sem framework.

Todos os dados são fictícios. Projeto independente, sem vínculo com a Justiça Eleitoral.

## Como rodar

```bash
./mvnw spring-boot:run
```

- Aplicação: http://localhost:8081
- Console do banco: http://localhost:8081/h2-console (JDBC URL `jdbc:h2:mem:votacao`, usuário `sa`, senha vazia)

No Windows, use `mvnw.cmd`. Se o Tomcat falhar ao subir com `Unable to establish loopback connection`, defina `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Temp/uds` (a pasta precisa existir).

```bash
./mvnw test
```

São 70 testes: unidade (Mockito), web (`@WebMvcTest`), integração (`@SpringBootTest` + H2 + Flyway), rotas do frontend e validação dos exercícios de SQL.

## Páginas

Cada terminal tem a sua rota. A seção vai na query string (`?eleicao=1&zona=3&secao=2`) e a navegação a preserva.

| Rota | Papel |
|---|---|
| `/` | Início e roteiro |
| `/mesario` | Terminal do mesário: abertura (zerésima), habilitação, caderno de votação, encerramento, RDV |
| `/urna` | Terminal do eleitor. Não tem controles de mesário: consulta o servidor a cada segundo e libera o voto quando o mesário habilita alguém |
| `/transmissao` | Ponto de transmissão: lê o BU assinado, permite editar o conteúdo e envia ao totalizador |
| `/resultados` | Apuração dos três cargos, com atualização automática |
| `/candidatos` | Cadastro com erros por campo, paginação e ordenação |

## Roteiro (5 minutos)

1. Em `/mesario`, **abra a urna** da zona 3, seção 2. A urna recebe um par de chaves Ed25519 e a zerésima prova que o RDV está vazio.
2. Abra `/urna` em **outra aba**. Ela mostra "aguardando liberação pelo mesário".
3. No mesário: **Próximo da fila → Habilitar.** A urna libera o voto. Um eleitor por vez, e quem já votou não é liberado de novo.
4. Na urna: digite o número e aperte **CONFIRMA**. A ordem é Senador (3 dígitos), Governador (2) e Presidente (2). Use **BRANCO** para votar em branco, **CORRIGE** para recomeçar o cargo, e um número inexistente vira **VOTO NULO**.
5. **Encerrar e gerar BU.** O BU é montado a partir do RDV e assinado com a chave privada da urna. **Ver RDV** mostra os votos em posições sorteadas, sem eleitor e sem horário.
6. Em `/transmissao`, altere qualquer número do conteúdo e transmita: a assinatura não confere e o BU é descartado. Restaure o original e transmita: o BU é aceito. Transmitir de novo responde 200 sem duplicar.
7. Em `/resultados`, a apuração chega a 100% das seções.

## Endpoints

| Método | Caminho | O quê |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/candidatos[/{id}]` | CRUD com validação, paginação e ordenação por lista branca |
| POST | `/api/boletins` | Recebe BU sem assinatura (carga de dados, testes) |
| POST | `/api/boletins/assinados` | Recebe BU assinado: verifica a assinatura, valida, totaliza; idempotente |
| GET | `/api/eleicoes/{id}/resultado?cargo=` | Apuração: válidos, brancos, nulos, situação de cada candidato |
| POST | `/api/eleicoes/{e}/zonas/{z}/secoes/{s}/urna/abertura` | Abre a urna e devolve a zerésima |
| GET | `…/urna`, `…/urna/cedula`, `…/urna/caderno?pendentes=` | Estado, candidatos na ordem da urna, caderno de votação |
| POST/DELETE | `…/urna/habilitacao` | Mesário habilita / cancela |
| POST | `…/urna/votos` | Voto completo do eleitor habilitado (não leva o título) |
| POST | `…/urna/encerramento` | Gera o BU a partir do RDV e assina |
| GET | `…/urna/boletim`, `…/urna/rdv` | BU assinado e RDV, só após o encerramento |

Exemplos prontos para executar estão em [requests.http](requests.http).

## Estrutura

```
apuracao/   regra de apuração (função pura) e consultas agregadas
boletim/    BU: entidade agregada, recepção comum e recepção assinada
candidato/  CRUD de referência para o live coding
comum/      exceções de negócio, ProblemDetail, paginação, rotas do frontend
legado/     código ruim para o exercício de refatoração
urna/       ciclo da urna: abertura, habilitação, RDV, encerramento, assinatura

resources/static/   uma página por rota; comum.js (API, navegação), estilo.css, urna.css
```

## Material de estudo

1. [docs/01-live-coding.md](docs/01-live-coding.md): exercícios cronometrados e perguntas prováveis sobre o código.
2. [docs/02-case-arquitetura.md](docs/02-case-arquitetura.md): case "recebimento e totalização de BUs" no roteiro de 7 passos.
3. [docs/03-refatoracao.md](docs/03-refatoracao.md): refatorar `ApuracaoLegada` com testes de caracterização.
4. [docs/04-sistema-eleitoral.md](docs/04-sistema-eleitoral.md): como o processo eleitoral brasileiro funciona, com fontes.
5. [sql/exercicios.sql](sql/exercicios.sql): 8 exercícios de SQL (respostas em `sql/respostas.sql`).
