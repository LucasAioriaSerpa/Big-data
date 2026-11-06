import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Settings:
    result_file: Path

    @classmethod
    def from_environment(cls) -> "Settings":
        configured_path = os.getenv(
            "RESULT_FILE",
            "../temperatura-media/src/main/resources/output/"
            "output-temperatura-correlacoes/part-r-00000",
        )
        return cls(result_file=Path(configured_path).expanduser())
