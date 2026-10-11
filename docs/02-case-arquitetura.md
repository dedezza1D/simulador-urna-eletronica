# Case: recebimento e totalização de boletins de urna

Enunciado típico: *"Desenhe o sistema que recebe os boletins de urna no dia da eleição e divulga o resultado."* Responda seguindo os 7 passos, em 30 a 40 minutos, desenhando enquanto fala.

## 1. Requisitos e volume

Pergunte antes de desenhar e diga as premissas em voz alta.

- **Quem escreve:** pontos de transmissão (cartórios e locais de votação), em rede privada. Não é a internet aberta.
- **Quem lê:** imprensa, partidos, público. É o maior volume de acessos da noite.
- **Ordem de grandeza (2026):** cerca de 159 milhões de eleitores aptos. Só São Paulo teve mais de 100 mil seções, então o país tem centenas de milhares de BUs, um por seção, cada um de poucos KB.
- **Pico de escrita:** concentrado nas primeiras horas após o fechamento. Exemplo de conta: 500 mil BUs em 3 horas ≈ 46/s em média. Dimensione para 10× (≈ 500/s) por causa das rajadas.
- **Pico de leitura:** milhões de consultas ao resultado, ordens de grandeza acima da escrita.
- **Crítico, em ordem:**
  1. **integridade:** nenhum BU adulterado, nenhum contado duas vezes;
  2. **auditabilidade:** tudo rastreável até a seção;
  3. **disponibilidade** na noite da eleição;
  4. latência da divulgação.

## 2. Modelo de dados

- `boletim_recebido`: o BU **bruto** como chegou (conteúdo, assinatura, hash, seção, data de recebimento). Só recebe inserções, nunca é alterado. É a fonte de verdade para auditoria.
- `boletim_urna`, `boletim_cargo`, `voto_candidato`: o BU já validado e normalizado (como neste projeto).
- `totais_por_cargo` / `totais_por_candidato`: agregados por abrangência (país, UF, município), para leitura rápida.
- **Chaves de unicidade:** `(eleicao, secao)` em `boletim_urna`, e `(eleicao, secao, hash)` em `boletim_recebido`.

## 3. API

- `POST /boletins/assinados`: idempotente pelo hash. Mesmo BU → 200; BU diferente para a mesma seção → 409 + alerta; assinatura inválida → 422 + alerta.
- `GET /resultado?cargo=&abrangencia=`: cacheável (ETag, `Cache-Control: max-age` curto).
- `GET /boletins/{secao}`: para conferência pública contra o BU impresso na porta da seção.

## 4. Componentes e integração

```
Ponto de transmissão ──(mTLS, rede privada)──> Receptor ──> fila ──> Totalizador ──> Publicador ──> CDN
                                                 │                       │
                                          boletim_recebido        totais agregados
```

- **Receptor (síncrono):** verifica a assinatura, grava o bruto, publica o evento (padrão **outbox**: evento e gravação na mesma transação) e responde. O ponto de transmissão precisa da confirmação na hora.
- **Totalizador (assíncrono):** consumidor **idempotente** que normaliza o BU e atualiza os agregados. Se cair, a fila segura os eventos e nada se perde.
- **Publicador:** a cada N segundos gera um *snapshot* do resultado em arquivos estáticos (JSON) e publica em CDN. A leitura nunca chega ao banco transacional.

**Ponto sênior: contenção nos totais.** Todo BU atualiza as mesmas linhas, por exemplo "total de Presidente no país". `UPDATE total = total + x` vira gargalo de lock. Alternativas:
- recalcular os agregados periodicamente com `SUM ... GROUP BY` sobre as tabelas normalizadas (é o que `ApuracaoRepository` faz);
- contadores particionados (por UF) somados na leitura;
- um único escritor por agregado (consumidor particionado por chave).

## 5. Segurança

- **Assinatura digital por urna**, com a chave pública cadastrada antes da eleição, na carga das urnas. BU sem assinatura válida é descartado. Implementado neste projeto: `AssinaturaDigital`, `ReceptorBoletimService`.
- **mTLS** entre ponto de transmissão e receptor, com certificado por ponto e revogação. Conecte com sua experiência em TLS/mTLS multi-tenant.
- **Rede privada** para escrita. A leitura pública fica em infraestrutura separada (CDN).
- **Menor privilégio:** o receptor só insere; o publicador só lê.
- **Trilha de auditoria** imutável e hashes publicados. Qualquer pessoa pode conferir BU impresso × BU publicado × RDV.
- Reduzir a superfície de ataque foi o motivo da centralização da totalização no TSE em 2020 (veja [04-sistema-eleitoral.md](04-sistema-eleitoral.md)).

## 6. Testes e observabilidade

- **Unidade:** regras de apuração (função pura `Apuracao`) e de consistência do BU.
- **Integração:** banco real (Testcontainers com Oracle/Postgres) e fila real.
- **Carga (JMeter):** com **volume e distribuição de dados realistas**, base populada e estatísticas atualizadas. Em 2020, o atraso foi atribuído em parte a planos de execução ruins gerados com a base vazia.
- **Ensaio geral** (simulado nacional) antes da eleição.
- **Métricas:** BUs recebidos/min, rejeitados por motivo (assinatura, consistência, duplicidade), atraso da fila, p95/p99 do receptor, % de seções totalizadas por UF.
- **Alertas:** qualquer assinatura inválida ou BU divergente é incidente, não só log.
- **Logs** correlacionados por seção, para rastrear um BU ponta a ponta.

## 7. Trade-offs e o que faria com mais tempo

| Decisão | Ganho | Custo |
|---|---|---|
| Totalização centralizada | menor superfície de ataque, um só ponto de verdade | ponto único de falha → exige redundância e plano de recuperação de desastre |
| Recepção síncrona + totalização assíncrona | confirmação imediata e resiliência a picos | consistência eventual na divulgação (segundos) |
| Snapshots em CDN | leitura escala sem tocar no banco | resultado defasado em N segundos |
| Recalcular agregados vs. incrementar | simples, correto, auditável | custo de CPU a cada ciclo; precisa de índices certos |

Com mais tempo: assinatura também nos snapshots publicados, verificação independente (outro time recontando a partir dos BUs), *chaos testing* no receptor e no totalizador, e um runbook para BU divergente.
