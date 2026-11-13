from dataclasses import dataclass


@dataclass(frozen=True)
class CentroidValue:
    name: str
    value: float


@dataclass(frozen=True)
class CorrelationValue:
    name: str
    value: float | None


@dataclass(frozen=True)
class ClusterResult:
    k: int
    cluster: int
    records: int
    centroid: tuple[CentroidValue, ...]
    correlations: tuple[CorrelationValue, ...]
    inertia: float
    silhouette: float
