#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/temperatura-media"
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