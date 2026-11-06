from pydantic import BaseModel

from app.features.results.domain.models import ClusterResult


class NamedValueResponse(BaseModel):
    name: str
    value: float | None


class ClusterResultResponse(BaseModel):
    k: int
    cluster: int
    records: int
    centroid: list[NamedValueResponse]
    correlations: list[NamedValueResponse]
    inertia: float

    @classmethod
    def from_domain(cls, result: ClusterResult) -> "ClusterResultResponse":
        return cls(
            k=result.k,
            cluster=result.cluster,
            records=result.records,
            centroid=[NamedValueResponse(name=item.name, value=item.value) for item in result.centroid],
            correlations=[
                NamedValueResponse(name=item.name, value=item.value)
                for item in result.correlations
            ],
            inertia=result.inertia,
        )


class ElbowPointResponse(BaseModel):
    k: int
    inertia: float
