from fastapi import FastAPI

from app.features.results.presentation.controllers import router

app = FastAPI(
    title="Gráfico API",
    description="Consulta os resultados do job temperatura-media.",
    version="0.1.0",
)
app.include_router(router)
