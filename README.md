# Big-data

utilizando hadoop 3.3.6 & jdk 11 com maven

---

## Configuração temporária para o hadoop

```bash
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH="$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$JAVA_HOME/bin:$PATH"
```

## Comandos para os projetos JAVA

### BigData-Mavem

```bash
cd BigData-Mavem/
mvn clean package
mvn package
rm -rf output-wordcount
hadoop jar target/BigData-Mavem-1.0-SNAPSHOT.jar \
    org.countWords.Main \
    src/main/resources/input/bible.txt \
    src/main/resources/output
cat src/main/resources/output/part-r-00000
cd ..
```

### Temperatura-media ( já otimizada )

```bash
cd temperatura-media/
mvn clean package
mvn package
rm -rf output-temperatura
hadoop jar target/temperatura-media-1.0-SNAPSHOT.jar \
    org.forestFire.Main \
    src/main/resources/input/forestfireinput.csv \
    src/main/resources/output
cat src/main/resources/output/part-r-00000
cd ..
```
