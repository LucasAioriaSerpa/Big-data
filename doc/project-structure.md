# Estrutura do projeto

## Classificação

Repositório **monorepo acadêmico pequeno**, composto por dois projetos Java
independentes do tipo **CLI/batch**. Não há backend HTTP, frontend, banco de
dados ou serviço residente.

## Módulos

### `BigData-Mavem`

Job Hadoop MapReduce de contagem de palavras.

- Entrada: `src/main/resources/input/bible.txt`.
- Saída: diretório informado no segundo argumento, normalmente
  `src/main/resources/output`.
- Entrada do programa: [`Main.main`](../BigData-Mavem/src/main/java/org/countWords/Main.java:15).
- Mapper: [`_Map.map`](../BigData-Mavem/src/main/java/org/countWords/utils/_Map.java:14).
- Reducer: [`_Reduce.reduce`](../BigData-Mavem/src/main/java/org/countWords/utils/_Reduce.java:12).

### `temperatura-media`

Job Hadoop MapReduce para agrupar registros de incêndios florestais por
clusters K-means++ e calcular correlações por cluster.

- Entrada padrão: `src/main/resources/input/forestfireinput.csv`.
- Saída padrão: `output-temperatura-correlacoes`.
- Entrada do programa: [`org.forestFire.Main.main`](../temperatura-media/src/main/java/org/forestFire/Main.java:20).
- Modelo/parser: [`FireRecord`](../temperatura-media/src/main/java/org/forestFire/data/FireRecord.java:5).
- Pipeline atual: [`ClusterMapper`](../temperatura-media/src/main/java/org/forestFire/clustering/ClusterMapper.java:10) e
  [`ClusterReducer`](../temperatura-media/src/main/java/org/forestFire/clustering/ClusterReducer.java:10).
- Componentes de correlação direta (`CorrelationMapper`, `CorrelationCombiner`
  e `CorrelationReducer`) existem, mas não são registrados pelo `Main` atual.

## Camadas observadas

1. **Entrada/orquestração:** classes `Main` e scripts Bash.
2. **MapReduce:** mappers, combiners e reducers Hadoop.
3. **Modelo/estado:** `FireRecord` e `CorrelationStats`.
4. **Persistência de arquivos:** `FileInputFormat`, `FileOutputFormat`,
   `FileSystem` e arquivos `part-r-00000`.

Não há camada de domínio separada, repositório, API ou configuração externa
estruturada.

## Dependências internas

- `BigData-Mavem`: `Main` -> `_Map`/`_Reduce`.
- `temperatura-media`: `Main` -> `FireRecord` + `ClusterMapper`/`ClusterReducer`;
  `ClusterMapper` e `ClusterReducer` dependem das constantes e do formato de
  `FireRecord`.
- `CorrelationStats` é usado apenas pelo pipeline de correlação direta legado,
  não pelo pipeline chamado pelo `Main`.

## Artefatos não-fonte

`target/`, classes compiladas, JARs, CRCs e diretórios de saída são artefatos
gerados. Eles não representam novas unidades funcionais.
