from frontend.app import create_app
from frontend.app.config import FrontendSettings


def test_dashboard_is_rendered() -> None:
    client = create_app(FrontendSettings("http://backend")).test_client()

    response = client.get("/")

    assert response.status_code == 200
    assert b"Curva do cotovelo" in response.data
    assert b"Silhueta por K" in response.data
    assert b"correlation-chart" in response.data


def test_proxy_rejects_invalid_k() -> None:
    client = create_app(FrontendSettings("http://backend")).test_client()

    response = client.get("/api/results?k=1")

    assert response.status_code == 400
