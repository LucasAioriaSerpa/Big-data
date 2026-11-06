from typing import Any

import requests


class BackendApiError(Exception):
    """Raised when the frontend cannot obtain valid data from FastAPI."""


class ResultsApiClient:
    def __init__(self, base_url: str, timeout_seconds: float = 5.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout_seconds = timeout_seconds

    def get_results(self, k: int | None = None) -> list[dict[str, Any]]:
        params = {"k": k} if k is not None else {}
        try:
            response = requests.get(
                f"{self._base_url}/api/v1/results",
                params=params,
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            payload = response.json()
        except (requests.RequestException, ValueError) as exc:
            raise BackendApiError("Não foi possível obter dados do FastAPI.") from exc
        if not isinstance(payload, list):
            raise BackendApiError("Resposta inválida recebida do FastAPI.")
        return payload

    def get_elbow(self) -> list[dict[str, Any]]:
        try:
            response = requests.get(
                f"{self._base_url}/api/v1/elbow",
                timeout=self._timeout_seconds,
            )
            response.raise_for_status()
            payload = response.json()
        except (requests.RequestException, ValueError) as exc:
            raise BackendApiError("Não foi possível obter a curva do cotovelo.") from exc
        if not isinstance(payload, list):
            raise BackendApiError("Resposta inválida recebida para a curva do cotovelo.")
        return payload
