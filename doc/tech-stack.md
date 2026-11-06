# Stack tecnológico existente

- Linguagem: Java.
- Nível de compilação declarado: Java 11 (`maven.compiler.source/target`).
- Ambiente validado nesta análise: Maven 3.9.16 e Java 25.0.4; os projetos
  compilam. O binário `hadoop` não está no PATH e a tentativa de execução
  local falha em `UserGroupInformation`/`Subject.getSubject`, incompatível com
  Java 25; usar Java 11 para validar o runtime Hadoop.
- Build: Maven, com um `pom.xml` por projeto.
- Processamento: Apache Hadoop 3.3.6.
- APIs usadas: Hadoop Common, MapReduce Client Core, Hadoop Client, `Mapper`,
  `Reducer`, `Job`, `FileSystem`, `FileInputFormat` e `FileOutputFormat`.
- Armazenamento: filesystem local quando configurado pelo job de contagem;
  Hadoop `FileSystem` no job de incêndios, permitindo caminho local/HDFS
  conforme a configuração.
- Empacotamento: JAR Maven; execução via `hadoop jar`.
- Scripts operacionais: Bash para configurar Java/Hadoop, compilar, remover
  saídas e executar os JARs.

## Dependências declaradas

Ambos os POMs declaram:

- `org.apache.hadoop:hadoop-common:3.3.6`
- `org.apache.hadoop:hadoop-mapreduce-client-core:3.3.6`
- `org.apache.hadoop:hadoop-client:3.3.6`

`BigData-Mavem` também declara explicitamente `maven-compiler-plugin` sem
versão fixa. Não há testes, framework web, ORM, mensageria, banco ou
serialização externa declarados.

## Restrições observadas

- A documentação e os scripts esperam Java 11 e uma instalação Hadoop em
  `/usr/local/hadoop`.
- O código de `BigData-Mavem` fixa `mapreduce.framework.name=local` e
  `fs.defaultFS=file:///`.
- O `temperatura-media` não fixa o framework no código; a configuração Hadoop
  determina o filesystem/runtime.
- Não há arquivo de propriedades, YAML de aplicação, container ou pipeline CI
  versionado.
