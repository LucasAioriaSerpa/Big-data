from pathlib import Path

import pytest

from app.features.results.infrastructure.result_parser import (
    NUMERIC_NAMES,
    ResultFileNotFoundError,
    ResultFormatError,
    ResultParser,
)

CORRELATION_NAMES = [
    f"{row}~{column}" for row in NUMERIC_NAMES for column in NUMERIC_NAMES
]
SAMPLE = (
    "k=2\t0\t517\t"
    "1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0,11.0\t"
    + ";".join(f"{name}=0.1" for name in CORRELATION_NAMES)
)


def test_parser_preserves_cluster_and_named_values() -> None:
    result = ResultParser().parse_line(SAMPLE)

    assert result.k == 2
    assert result.cluster == 0
    assert result.records == 517
    assert result.centroid[0].name == "X"
    assert result.correlations[6].name == "X~temp"
    assert result.correlations[6].value == 0.1


def test_parser_maps_indefinida_to_none() -> None:
    line = SAMPLE.rsplit("\t", 1)[0] + "\t" + ";".join(["indefinida"] * 121)

    result = ResultParser().parse_line(line)

    assert result.correlations[0].name == "X~X"
    assert result.correlations[0].value is None


def test_parser_rejects_missing_file(tmp_path: Path) -> None:
    with pytest.raises(ResultFileNotFoundError):
        ResultParser().parse_file(tmp_path / "missing")


def test_parser_rejects_wrong_field_count() -> None:
    with pytest.raises(ResultFormatError, match="5, 6 ou 7 campos"):
        ResultParser().parse_line("k=2\t0\t1")
