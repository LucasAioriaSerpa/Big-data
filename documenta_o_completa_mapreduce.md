# Documentação completa: correlações com Hadoop MapReduce

Este documento descreve a versão atual do projeto `temperatura-media`. O job
MapReduce lê o arquivo `forestfireinput.csv` e calcula a correlação de Pearson
entre a temperatura e cada variável selecionada do dataset.

O resultado não é mais uma média global de temperatura. Para cada variável, o
job produz:

```text
r=<correlação de Pearson>; n=<quantidade de registros>
```

Exemplo:

```text
coluna 7 - DC    r=0.496208; n=517
```

## 1. Arquitetura do job

O fluxo possui quatro partes:

1. **Mapper:** valida cada linha do CSV e emite estatísticas parciais para cada
   variável comparada com a temperatura.
2. **Combiner:** agrega estatísticas localmente no nó do Mapper, reduzindo o
   volume transferido durante o shuffle.
3. **Shuffle:** agrupa os valores pela chave da variável, como
   `coluna 7 - DC` ou `coluna 3 - mes=aug`.
4. **Reducer:** combina os estados recebidos, calcula a correlação final e
   grava uma linha por variável.

O estado estatístico é associativo: ele pode ser calculado em partes e depois
mesclado sem carregar todos os registros na memória. Isso permite executar a
mesma lógica localmente ou em um cluster Hadoop.

## 2. Configuração do job

Arquivo: `temperatura-media/src/main/java/org/forestFire/Main.java`

O método `main` recebe exatamente dois argumentos:

```text
<caminho_entrada> <diretorio_saida>
```

O job configura:

```java
Configuration conf = new Configuration();
Job job = Job.getInstance(conf, "Correlacoes com Temperatura");
```

As classes do pipeline são registradas da seguinte forma:

```java
job.setMapperClass(Map.class);
job.setCombinerClass(Combine.class);
job.setReducerClass(Reduce.class);
```

O Mapper e o Combiner usam `Text` como chave e valor. O Reducer também escreve
`Text` na saída:

```java
job.setMapOutputKeyClass(Text.class);
job.setMapOutputValueClass(Text.class);
job.setOutputKeyClass(Text.class);
job.setOutputValueClass(Text.class);
```

Os caminhos são adicionados ao job com `FileInputFormat` e
`FileOutputFormat`. O Hadoop exige que o diretório de saída não exista antes
da execução.

## 3. Mapper: validação e emissão

Arquivo: `temperatura-media/src/main/java/org/forestFire/utils/Map.java`

O dataset possui 13 colunas. A temperatura está na coluna de índice 8, que
corresponde à nona coluna do CSV:

```text
0: X
1: Y
2: month
3: day
4: FFMC
5: DMC
6: DC
7: ISI
8: temp
9: RH
10: wind
11: rain
12: area
```

O Mapper executa estas validações:

- exige exatamente 13 colunas;
- converte temperatura e variáveis numéricas para `double`;
- rejeita valores não finitos (`NaN` e infinito);
- verifica se o mês pertence à lista `jan` até `dec`;
- verifica se o dia pertence à lista `mon` até `sun`;
- contabiliza linhas inválidas no contador Hadoop
  `CSV.REGISTROS_INVALIDOS`.

Linhas inválidas são descartadas sem interromper o job.

### Variáveis numéricas

Para cada registro válido, o Mapper emite a temperatura contra estas colunas:

```text
coluna 1 - X
coluna 2 - Y
coluna 5 - FFMC
coluna 6 - DMC
coluna 7 - DC
coluna 8 - ISI
coluna 10 - RH
coluna 11 - wind
coluna 12 - rain
coluna 13 - area
```

### Variáveis categóricas

Meses e dias são transformados em variáveis indicadoras. Para cada registro,
o Mapper emite todas as categorias:

```text
coluna 3 - mes=jan
...
coluna 3 - mes=dec
coluna 4 - dia=mon
...
coluna 4 - dia=sun
```

O valor da categoria é `1.0` quando o registro pertence à categoria e `0.0`
caso contrário. Assim, também é possível calcular a correlação entre
temperatura e a ocorrência de cada mês ou dia.

## 4. Estado estatístico incremental

Arquivo: `temperatura-media/src/main/java/org/forestFire/utils/CorrelationStats.java`

Para cada par:

```text
x = temperatura
y = variável analisada
```

o sistema armazena:

```text
count
meanX
meanY
varianceX
varianceY
covariance
```

Para adicionar um registro, o código usa atualização incremental no estilo
Welford. Isso evita guardar uma lista de valores e reduz problemas numéricos
em comparação com calcular diretamente grandes somas de quadrados.

O estado é serializado como:

```text
count,meanX,meanY,varianceX,varianceY,covariance
```

Por exemplo, o Mapper cria um estado com um único par e o envia como valor:

```java
CorrelationStats stats = new CorrelationStats();
stats.add(temperature, value);
context.write(new Text(feature), new Text(stats.serialize()));
```

## 5. Combiner: agregação local

Arquivo: `temperatura-media/src/main/java/org/forestFire/utils/Combine.java`

O Combiner recebe vários estados da mesma variável no mesmo Mapper. Ele:

1. desserializa cada estado com `CorrelationStats.parse`;
2. combina os estados usando `CorrelationStats.merge`;
3. serializa um único estado parcial;
4. emite novamente a mesma chave.

O Combiner não deve produzir o resultado final. Ele deve preservar o mesmo
formato de estado aceito pelo Reducer. O Hadoop pode executar o Combiner zero,
uma ou várias vezes, por isso a operação de combinação precisa ser segura para
ser repetida.

## 6. Reducer: cálculo da correlação

Arquivo: `temperatura-media/src/main/java/org/forestFire/utils/Reduce.java`

Para cada variável, o Reducer combina todos os estados parciais recebidos.
Depois aplica a fórmula da correlação de Pearson:

```text
r = covariance / sqrt(varianceX * varianceY)
```

O resultado é escrito com seis casas decimais:

```java
String.format(
    java.util.Locale.ROOT,
    "r=%.6f; n=%d",
    correlation,
    combined.getCount()
)
```

Há dois casos tratados explicitamente:

- menos de dois registros: `indefinida (menos de 2 registros)`;
- variância zero ou correlação não finita: `indefinida (variancia zero)`.

## 7. Compilação e execução

O projeto usa Hadoop 3.3.6 nas dependências Maven e Java 11 como nível de
compilação. O ambiente precisa ter Hadoop configurado:

```bash
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$PATH"
```

Verifique:

```bash
java -version
mvn -version
hadoop version
```

Compile o projeto:

```bash
cd /workspaces/Big-data/temperatura-media
mvn clean package
```

Execute diretamente com o caminho atual do dataset:

```bash
rm -rf output-temperatura-correlacoes

hadoop jar target/temperatura-media-1.0-SNAPSHOT.jar \
  org.forestFire.Main \
  src/main/resources/input/forestfireinput.csv \
  output-temperatura-correlacoes
```

Leia o resultado bruto:

```bash
hadoop fs -cat output-temperatura-correlacoes/part-r-00000
```

## 8. Script automatizado

O script [`hadoop_temperatura-media.bash`](./hadoop_temperatura-media.bash)
carrega automaticamente a configuração em
[`hadoop_temp_config.bash`](./hadoop_temp_config.bash), compila o projeto,
remove a saída anterior, executa o job e formata a saída:

```bash
cd /workspaces/Big-data
./hadoop_temperatura-media.bash
```

O script aceita entrada e saída alternativas:

```bash
./hadoop_temperatura-media.bash \
  temperatura-media/src/main/resources/input/forestfireinput.csv \
  output-temperatura-correlacoes
```

## 9. Resultado validado

Com os 517 registros válidos do arquivo
`src/main/resources/input/forestfireinput.csv`, a execução produziu, entre
outros, os seguintes resultados:

| Variável | Correlação (r) | n |
|---|---:|---:|
| coluna 1 - X | -0.051258 | 517 |
| coluna 2 - Y | -0.024103 | 517 |
| coluna 5 - FFMC | 0.431532 | 517 |
| coluna 6 - DMC | 0.469594 | 517 |
| coluna 7 - DC | 0.496208 | 517 |
| coluna 8 - ISI | 0.394287 | 517 |
| coluna 10 - RH | -0.527390 | 517 |
| coluna 11 - wind | -0.227116 | 517 |
| coluna 12 - rain | 0.069491 | 517 |
| coluna 13 - area | 0.097844 | 517 |
| coluna 3 - mes=aug | 0.351404 | 517 |
| coluna 3 - mes=mar | -0.341797 | 517 |
| coluna 4 - dia=wed | 0.090580 | 517 |

Uma correlação positiva indica que a variável tende a aumentar junto com a
temperatura; uma correlação negativa indica tendência inversa. Correlação
próxima de zero indica pouca relação linear no conjunto analisado. Correlação
não implica causalidade.

## 10. Escalabilidade e extensões

O estado incremental permite processar arquivos maiores sem manter todos os
registros em memória. O Combiner reduz o tráfego de shuffle, e o Reducer
calcula uma saída independente para cada variável.

Para adicionar uma nova variável:

1. inclua o índice no array `NUMERIC_COLUMNS`;
2. inclua o nome correspondente em `NUMERIC_NAMES`;
3. garanta que a coluna seja numérica;
4. recompile e execute o job.

Para executar em um cluster, os caminhos de entrada e saída podem ser
substituídos por caminhos HDFS. A lógica do Mapper, Combiner e Reducer
permanece a mesma; apenas a configuração do Hadoop e o sistema de arquivos
utilizado mudam.
