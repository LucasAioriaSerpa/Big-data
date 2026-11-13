# Modelo de dados existente

## Entidades e registros

### `FireRecord`

Fonte: [`FireRecord`](../temperatura-media/src/main/java/org/forestFire/data/FireRecord.java:5).
É um registro imutável em memória, sem ORM ou persistência relacional.

- Colunas CSV: 13.
- Campos categóricos: `month`, `day`.
- Valores numéricos: `X`, `Y`, `FFMC`, `DMC`, `DC`, `ISI`, `temp`, `RH`,
  `wind`, `rain`, `area`.
- O parser exige exatamente 13 colunas, valores finitos e mês/dia válidos.
- As 11 variáveis numéricas, incluindo `area`, participam das dimensões de
  clustering (`CLUSTER_FEATURE_COUNT`).
- O clustering emite uma matriz de correlação 11×11 por cluster, com os pares
  no formato `variavel_linha~variavel_coluna=valor`.

### `CorrelationStats`

Fonte: [`CorrelationStats`](../temperatura-media/src/main/java/org/forestFire/correlation/CorrelationStats.java:5).
É um estado estatístico serializável, não uma entidade persistente.

- `count`
- `meanX`, `meanY`
- `varianceX`, `varianceY`
- `covariance`

Não existem relacionamentos, chaves, tabelas, transações ou mapeamentos ORM.

## Formatos de saída

- Contagem: chave textual da palavra e valor inteiro.
- Clustering: `k`, cluster, quantidade, centróide serializado e correlações
  separadas por tabulação/ponto e vírgula.
- Correlação direta legado: `r=<valor>; n=<quantidade>` ou mensagem
  `indefinida`.

## Resumo de entidades-chave

1. `FireRecord`: linha validada do dataset Forest Fires.
2. `CorrelationStats`: acumulador associativo para correlação de Pearson.
3. Palavra normalizada: chave textual agregada no job de contagem.
