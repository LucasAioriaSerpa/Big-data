from fastapi import APIRouter, HTTPException, Query

from app.core.config import Settings
from app.features.results.infrastructure.result_parser import ResultSourceError
from app.features.results.presentation.schemas import (
    ClusterResultResponse,
    ElbowPointResponse,
)
from app.features.results.services.result_service import ResultService

router = APIRouter()


def _service() -> ResultService:
    return ResultService(Settings.from_environment().result_file)


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/api/v1/results", response_model=list[ClusterResultResponse])
def list_results(
    k: int | None = Query(default=None, ge=2),
    cluster: int | None = Query(default=None, ge=0),
) -> list[ClusterResultResponse]:
    try:
        results = _service().list_results(k=k, cluster=cluster)
    except ResultSourceError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return [ClusterResultResponse.from_domain(result) for result in results]


@router.get("/api/v1/results/{k}", response_model=list[ClusterResultResponse])
def list_results_by_k(k: int) -> list[ClusterResultResponse]:
    return list_results(k=k)


@router.get("/api/v1/elbow", response_model=list[ElbowPointResponse])
def elbow() -> list[ElbowPointResponse]:
    try:
        points = _service().elbow()
    except ResultSourceError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return [ElbowPointResponse(k=k, inertia=inertia) for k, inertia in points]


@router.get("/api/v1/results/{k}/{cluster}", response_model=ClusterResultResponse)
def get_result(k: int, cluster: int) -> ClusterResultResponse:
    results = list_results(k=k, cluster=cluster)
    if not results:
        raise HTTPException(status_code=404, detail="Resultado não encontrado")
    return results[0]
