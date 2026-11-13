from pathlib import Path

from app.features.results.domain.models import ClusterResult
from app.features.results.infrastructure.result_parser import ResultParser


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
