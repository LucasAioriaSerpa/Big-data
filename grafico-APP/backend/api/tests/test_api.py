from pathlib import Path

from fastapi.testclient import TestClient

from app.main import app


def test_health() -> None:
    response = TestClient(app).get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_results_endpoint(monkeypatch, tmp_path: Path) -> None:
    source = tmp_path / "part-r-00000"
    source.write_text(
        "k=2\t0\t0\t0,0,0,0,0,0,0,0,0,0\t" + ";".join(["indefinida"] * 29) + "\n",
        encoding="utf-8",
    )
    monkeypatch.setenv("RESULT_FILE", str(source))

    response = TestClient(app).get("/api/v1/results/2/0")

    assert response.status_code == 200
    assert response.json()["records"] == 0
    assert response.json()["correlations"][0] == {"name": "X", "value": None}


def test_elbow_endpoint_sums_inertia_by_k(monkeypatch, tmp_path: Path) -> None:
    source = tmp_path / "part-r-00000"
    empty_correlations = ";".join(["indefinida"] * 29)
    lines = [
        f"k=2\t0\t1\t0,0,0,0,0,0,0,0,0,0\t{empty_correlations}\t1.5",
        f"k=2\t1\t1\t0,0,0,0,0,0,0,0,0,0\t{empty_correlations}\t2.5",
        f"k=3\t0\t1\t0,0,0,0,0,0,0,0,0,0\t{empty_correlations}\t0.75",
    ]
    source.write_text("\n".join(lines) + "\n", encoding="utf-8")
    monkeypatch.setenv("RESULT_FILE", str(source))

    response = TestClient(app).get("/api/v1/elbow")

    assert response.status_code == 200
    assert response.json() == [
        {"k": 2, "inertia": 4.0},
        {"k": 3, "inertia": 0.75},
    ]


def test_missing_source_returns_service_unavailable(monkeypatch, tmp_path: Path) -> None:
    monkeypatch.setenv("RESULT_FILE", str(tmp_path / "missing"))

    response = TestClient(app).get("/api/v1/results")

    assert response.status_code == 503
