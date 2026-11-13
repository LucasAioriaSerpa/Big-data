from pathlib import Path

import pytest

from app.features.results.infrastructure.result_parser import (
    ResultFileNotFoundError,
    ResultFormatError,
    ResultParser,
)

SAMPLE = (
    "k=2\t0\t517\t"
    "1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0\t"
    "X=0.1;Y=0.2;FFMC=0.3;DMC=0.4;DC=0.5;ISI=0.6;RH=0.7;"
    "wind=0.8;rain=0.9;area=1.0;month=jan=0.1;month=feb=0.2;"
    "month=mar=0.3;month=apr=0.4;month=may=0.5;month=jun=0.6;"
    "month=jul=0.7;month=aug=0.8;month=sep=0.9;month=oct=1.0;"
    "month=nov=1.1;month=dec=1.2;day=mon=1.3;day=tue=1.4;"
    "day=wed=1.5;day=thu=1.6;day=fri=1.7;day=sat=1.8;day=sun=1.9"
)


def test_parser_preserves_cluster_and_named_values() -> None:
    result = ResultParser().parse_line(SAMPLE)

    assert result.k == 2
    assert result.cluster == 0
    assert result.records == 517
    assert result.centroid[0].name == "X"
    assert result.correlations[10].name == "month=jan"
    assert result.correlations[10].value == 0.1


def test_parser_maps_indefinida_to_none() -> None:
    line = SAMPLE.rsplit("\t", 1)[0] + "\t" + ";".join(["indefinida"] * 29)

    result = ResultParser().parse_line(line)

    assert result.correlations[0].name == "X"
    assert result.correlations[0].value is None


def test_parser_rejects_missing_file(tmp_path: Path) -> None:
    with pytest.raises(ResultFileNotFoundError):
        ResultParser().parse_file(tmp_path / "missing")


def test_parser_rejects_wrong_field_count() -> None:
    with pytest.raises(ResultFormatError, match="5, 6 ou 7 campos"):
        ResultParser().parse_line("k=2\t0\t1")
