# Refatoração: `ApuracaoLegada`

[`legado/ApuracaoLegada.java`](../src/main/java/com/estudo/votacao/legado/ApuracaoLegada.java) apura um cargo majoritário. Funciona na maioria dos casos, mas é difícil de ler, tem efeitos colaterais e esconde defeitos de regra de negócio.

**Tarefa (30 min):** refatore sem mudar o comportamento. Depois corrija os defeitos de propósito, um por vez, ajustando o teste correspondente.

## Passo 0: rede de segurança

[`ApuracaoLegadaCaracterizacaoTest`](../src/test/java/com/estudo/votacao/legado/ApuracaoLegadaCaracterizacaoTest.java) já registra o comportamento atual, defeitos incluídos (marcados com `DEFEITO`). Rode antes de mexer. Numa entrevista, diga: *"antes de refatorar código sem teste, escrevo testes de caracterização"*.

## Passo 1: encontre os problemas

Tente listar antes de abrir a resposta.

<details>
<summary>Problemas (abra depois de tentar)</summary>

**Defeitos de regra de negócio**
1. Brancos entram no total (`t = t + b`). Votos válidos excluem brancos e nulos, então o percentual sai menor e pode forçar um 2º turno indevido.
2. `p >= 50`: exatamente 50% não é maioria absoluta. Deveria ser "mais da metade".
3. Sem votos, um cargo com 2º turno devolve lista vazia em silêncio, e o Senador elege o primeiro da lista.

**Robustez**

4. Lista vazia com brancos estoura `IndexOutOfBoundsException`.
5. `c.equals("SENADOR")` com `c` nulo estoura NPE. String mágica no lugar de tipo.
6. `Map<String, Object>` com casts: um `"v"` ausente ou de outro tipo explode em tempo de execução.

**Design**

7. Efeito colateral: ordena a lista **do chamador**.
8. Parâmetro `n` (nulos) nunca é usado.
9. Bubble sort manual, quando `List.sort` / `Comparator` resolvem.
10. Três laços quase iguais para montar o resultado (duplicação).
11. Saída como `String` `"nome;STATUS"`: quem consome precisa fazer parse. Cálculo e formatação misturados.
12. `double` para percentual, com risco de arredondamento na comparação.
13. Nomes de uma letra e comentários que dizem *o quê*, não *por quê*.

</details>

## Passo 2: sequência sugerida (passos pequenos, testes verdes a cada um)

1. **Renomear:** `l` → `candidatos`, `t` → `total`, `c` → `cargo`, `proc` → `apurar`.
2. **Parar de mutar a entrada:** ordenar uma cópia (`new ArrayList<>(candidatos)`). O teste "altera lista do chamador" quebra **de propósito**: atualize-o e diga isso em voz alta.
3. **Trocar o bubble sort** por `sort(Comparator.comparingInt(...).reversed())`.
4. **Extrair a decisão de situação** para um método `situacao(cargo, posicao, votosDoLider, total)` e remover os três laços duplicados.
5. **Introduzir tipos:** `enum Cargo` (com `temSegundoTurno`), `record TotalCandidato(String nome, long votos)`, `enum Situacao`. Resultado como `List<ResultadoCandidato>`, não `String`.
6. **Corrigir os defeitos**, um commit por defeito, cada um com o teste ajustado:
   - total = só votos nominais;
   - `votosDoLider * 2 > validos`;
   - sem votos → `INDEFINIDA`.

## Passo 3: compare com a versão final

[`apuracao/Apuracao.java`](../src/main/java/com/estudo/votacao/apuracao/Apuracao.java) é o destino dessa refatoração: função pura, tipos explícitos, sem efeito colateral, regra correta. [`ApuracaoTest`](../src/test/java/com/estudo/votacao/apuracao/ApuracaoTest.java) cobre os casos que o legado errava.

## O que falar durante o exercício

- Por que caracterizar antes de refatorar.
- A diferença entre **refatorar** (mesmo comportamento) e **corrigir** (comportamento novo). Nunca as duas coisas no mesmo passo.
- Que um defeito encontrado durante a refatoração deve ser levado ao time ou ao negócio antes de corrigir. Pode haver alguém dependendo do comportamento errado.
