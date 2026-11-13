from pathlib import Path

from app.features.results.domain.models import (
    CentroidValue,
    ClusterResult,
    CorrelationValue,
)

NUMERIC_NAMES = ("X", "Y", "FFMC", "DMC", "DC", "ISI", "temp", "RH", "wind", "rain", "area")
CENTROID_NAMES = NUMERIC_NAMES
CORRELATION_NAMES = tuple(
    f"{row}~{column}" for row in NUMERIC_NAMES for column in NUMERIC_NAMES
)


class ResultSourceError(Exception):
    """Base error for invalid or unavailable Hadoop result sources."""


class ResultFileNotFoundError(ResultSourceError):
    """Raised when the configured result file does not exist."""


class ResultFormatError(ResultSourceError):
    """Raised when a result line does not match the Hadoop contract."""


class ResultParser:
    """Parses the tab-separated output emitted by ClusterReducer."""

    def parse_file(self, path: Path) -> tuple[ClusterResult, ...]:
        if not path.is_file():
            raise ResultFileNotFoundError(f"Arquivo de resultados não encontrado: {path}")

        results: list[ClusterResult] = []
        try:
            with path.open(encoding="utf-8") as result_file:
                for line_number, line in enumerate(result_file, start=1):
                    if line.strip():
                        results.append(self.parse_line(line.rstrip("\n"), line_number))
        except OSError as exc:
            raise ResultSourceError(f"Não foi possível ler o arquivo: {path}") from exc
        return tuple(results)

    def parse_line(self, line: str, line_number: int = 1) -> ClusterResult:
        fields = line.split("\t")
        if len(fields) not in (5, 6, 7):
            raise ResultFormatError(
                f"Linha {line_number}: esperados 5, 6 ou 7 campos tabulados, encontrados {len(fields)}"
            )

        try:
            k = self._parse_k(fields[0], line_number)
            cluster = int(fields[1])
            records = int(fields[2])
            if cluster < 0 or records < 0:
                raise ValueError("cluster e registros devem ser não negativos")
            centroid = self._parse_centroid(fields[3], line_number)
            correlations = self._parse_correlations(fields[4], line_number)
            inertia = float(fields[5]) if len(fields) >= 6 else 0.0
            silhouette = float(fields[6]) if len(fields) == 7 else 0.0
            if inertia < 0:
                raise ValueError("inércia deve ser não negativa")
            if not -1.0 <= silhouette <= 1.0:
                raise ValueError("silhouette deve estar entre -1 e 1")
        except ValueError as exc:
            raise ResultFormatError(f"Linha {line_number}: {exc}") from exc

        return ClusterResult(k, cluster, records, centroid, correlations, inertia, silhouette)

    @staticmethod
    def _parse_k(value: str, line_number: int) -> int:
        if not value.startswith("k="):
            raise ValueError(f"campo k inválido na linha {line_number}")
        return int(value.removeprefix("k="))

    @staticmethod
    def _parse_centroid(value: str, line_number: int) -> tuple[CentroidValue, ...]:
        values = value.split(",")
        if len(values) != len(CENTROID_NAMES):
            raise ValueError(
                f"linha {line_number}: centróide deve conter {len(CENTROID_NAMES)} valores"
            )
        return tuple(
            CentroidValue(name, float(raw_value))
            for name, raw_value in zip(CENTROID_NAMES, values, strict=True)
        )

    @staticmethod
    def _parse_correlations(value: str, line_number: int) -> tuple[CorrelationValue, ...]:
        values = value.split(";")
        if len(values) != len(CORRELATION_NAMES):
            raise ValueError(
                f"linha {line_number}: correlações devem conter "
                f"{len(CORRELATION_NAMES)} valores"
            )

        correlations: list[CorrelationValue] = []
        for expected_name, raw_value in zip(CORRELATION_NAMES, values, strict=True):
            if raw_value == "indefinida":
                correlations.append(CorrelationValue(expected_name, None))
                continue
            name, separator, numeric_value = raw_value.rpartition("=")
            if not separator or name != expected_name:
                raise ValueError(
                    f"linha {line_number}: correlação inválida para {expected_name}"
                )
            if numeric_value == "indefinida":
                correlations.append(CorrelationValue(expected_name, None))
                continue
            correlations.append(CorrelationValue(name, float(numeric_value)))
        return tuple(correlations)
