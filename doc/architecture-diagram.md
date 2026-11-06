# Diagrama de arquitetura e fluxo

```mermaid
flowchart TD
  S[Scripts Bash / CLI] --> W[BigData-Mavem Main]
  W --> WI[bible.txt]
  W --> WM[Word Mapper]
  WM --> WS[Shuffle por palavra]
  WS --> WR[Word Reducer]
  WR --> WO[part-r-00000: palavra\\tcontagem]

  S --> F[temperatura-media Main]
  F --> FI[forestfireinput.csv]
  F --> PRE[Leitura válida + médias/desvios]
  PRE --> KP[K-means++ k=2..4]
  KP --> CM[ClusterMapper]
  CM --> CS[Shuffle por cluster]
  CS --> CR[ClusterReducer]
  CR --> IR[Resultado intermediário por iteração]
  IR --> KP
  CR --> FO[Saída final: cluster, n, centróide, correlações]

  LEG[CorrelationMapper/Combiner/Reducer] -. código presente, não registrado pelo Main .-> F
```

## Fluxos principais

1. O job de palavras força execução local e grava a agregação textual.
2. O job de incêndios lê os registros válidos antes do MapReduce para calcular
   normalização; em seguida executa jobs intermediários até convergência ou 50
   iterações para cada `k` de 2 a 4.
3. O reducer de clustering calcula simultaneamente centróides normalizados e
   correlações temperatura-versus-variável por cluster.
4. O filesystem é a única fronteira de dados entre processo e job. Não há
   estado compartilhado entre os dois projetos.
