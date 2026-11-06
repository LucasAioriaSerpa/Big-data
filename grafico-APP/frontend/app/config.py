import os
from dataclasses import dataclass


@dataclass(frozen=True)
class FrontendSettings:
    api_url: str = "http://127.0.0.1:8000"

    @classmethod
    def from_environment(cls) -> "FrontendSettings":
        return cls(api_url=os.getenv("FASTAPI_URL", cls.api_url).rstrip("/"))
