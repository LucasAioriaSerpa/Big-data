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
#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/temperatura-media"
if [[ -f "$SCRIPT_DIR/hadoop_temp_config.bash" ]]; then
    source "$SCRIPT_DIR/hadoop_temp_config.bash"
fi
if ! command -v hadoop >/dev/null 2>&1; then
    printf 'Erro: Hadoop não encontrado. Configure HADOOP_HOME e PATH antes de executar.\n' >&2
    exit 127
fi
cd "$PROJECT_DIR"
INPUT_PATH="${1:-src/main/resources/input/forestfireinput.csv}"
OUTPUT_PATH="${2:-output-temperatura-correlacoes}"
mvn clean package
hadoop fs -rm -r -f "$OUTPUT_PATH"
hadoop jar target/temperatura-media-1.0-SNAPSHOT.jar \
    org.forestFire.Main \
    "$INPUT_PATH" \
    "$OUTPUT_PATH"
hadoop fs -cat "$OUTPUT_PATH/part-r-00000" | awk -F '\t' '
BEGIN {
    printf "%-26s | %14s | %5s\n", "Variavel", "Correlacao (r)", "n"
    printf "%-26s-+-%14s-+-%5s\n", "--------------------------", "--------------", "-----"
}
{
    split($2, stats, ";")
    split(stats[1], correlation, "=")
    split(stats[2], count, "=")
    gsub(/^[[:space:]]+|[[:space:]]+$/, "", count[2])
    printf "%-26s | %14s | %5s\n", $1, correlation[2], count[2]
}'
cd ..
```
