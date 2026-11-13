from pathlib import Path

from app.features.results.domain.models import ClusterResult
from app.features.results.infrastructure.result_parser import (
    NUMERIC_NAMES,
    ResultParser,
    ResultSourceError,
)


class ResultService:
    """Coordinates loading and filtering result records."""

    def __init__(self, result_file: Path, parser: ResultParser | None = None) -> None:
        self._result_file = result_file
        self._parser = parser or ResultParser()

    def list_results(self, k: int | None = None, cluster: int | None = None) -> tuple[ClusterResult, ...]:
        results = self._parser.parse_file(self._result_file)
        return tuple(
            result
            for result in results
            if (k is None or result.k == k)
            and (cluster is None or result.cluster == cluster)
        )

    def elbow(self) -> tuple[tuple[int, float], ...]:
        results = self._parser.parse_file(self._result_file)
        grouped: dict[int, float] = {}
        for result in results:
            grouped[result.k] = grouped.get(result.k, 0.0) + result.inertia
        return tuple(sorted(grouped.items()))

    def silhouette(self) -> tuple[tuple[int, float], ...]:
        results = self._parser.parse_file(self._result_file)
        grouped: dict[int, list[float]] = {}
        for result in results:
            grouped.setdefault(result.k, []).append(result.silhouette)
        return tuple(
            (k, sum(scores) / len(scores))
            for k, scores in sorted(grouped.items())
            if scores
        )

    def markdown_report(self) -> str:
        results = self._parser.parse_file(self._result_file)
        if not results:
            raise ResultSourceError("Nenhum resultado disponível para gerar o relatório.")

        silhouette_points = self.silhouette()
        best_k, best_score = max(silhouette_points, key=lambda point: (point[1], -point[0]))
        best_results = tuple(result for result in results if result.k == best_k)

        lines = [
            "# Relatório de análise — Forest Fire",
            "",
            "## Metodologia",
            "",
            "- Dados processados pelo job Hadoop `temperatura-media`.",
            "- As 11 variáveis numéricas foram normalizadas com z-score.",
            "- Foi executado K-Means++ para K de 2 a 10.",
            "- A qualidade foi avaliada pelo coeficiente Silhouette.",
            "- As correlações são de Pearson e foram calculadas dentro de cada cluster.",
            "",
            "## Seleção do melhor K",
            "",
            "| K | Silhouette |",
            "|---:|---:|",
        ]
        lines.extend(f"| {k} | {score:.6f} |" for k, score in silhouette_points)
        lines.extend(
            [
                "",
                f"**Melhor K:** {best_k}, com Silhouette médio de **{best_score:.6f}**.",
                "",
                "## Clusters do melhor K",
                "",
                "| Cluster | Registros | Inércia |",
                "|---:|---:|---:|",
            ]
        )
        lines.extend(
            f"| {result.cluster} | {result.records} | {result.inertia:.6f} |"
            for result in best_results
        )

        lines.extend(["", "## Matrizes de correlação", ""])
        for result in best_results:
            lines.extend(
                [
                    f"### Cluster {result.cluster}",
                    "",
                    self._correlation_table(result),
                    "",
                ]
            )

        lines.extend(["## Observações e hipóteses", ""])
        for result in best_results:
            strongest = self._strongest_correlation(result)
            if strongest is None:
                lines.append(
                    f"- Cluster {result.cluster}: não há correlação definida suficiente para interpretação."
                )
                continue
            pair, value = strongest
            direction = "positiva" if value > 0 else "negativa"
            lines.append(
                f"- Cluster {result.cluster}: a relação mais intensa foi **{pair}** "
                f"({value:.6f}), uma associação {direction}. Isso é uma hipótese "
                "descritiva, não uma relação causal."
            )
        return "\n".join(lines) + "\n"

    @staticmethod
    def _correlation_table(result: ClusterResult) -> str:
        values = {item.name: item.value for item in result.correlations}
        header = "| Variável | " + " | ".join(NUMERIC_NAMES) + " |"
        separator = "|" + "---|" * (len(NUMERIC_NAMES) + 1)
        rows = [header, separator]
        for row in NUMERIC_NAMES:
            cells = [
                "indefinida"
                if values.get(f"{row}~{column}") is None
                else f"{values[f'{row}~{column}']:.6f}"
                for column in NUMERIC_NAMES
            ]
            rows.append("| " + row + " | " + " | ".join(cells) + " |")
        return "\n".join(rows)

    @staticmethod
    def _strongest_correlation(result: ClusterResult) -> tuple[str, float] | None:
        candidates = [
            (item.name, item.value)
            for item in result.correlations
            if item.value is not None
            and "~" in item.name
            and item.name.split("~", 1)[0] != item.name.split("~", 1)[1]
        ]
        if not candidates:
            return None
        return max(candidates, key=lambda item: abs(item[1]))
