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