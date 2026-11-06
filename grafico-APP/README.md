# grafico-APP

Aplicação local para visualizar os resultados do job `temperatura-media`.

```text
grafico-APP/
├── backend/api/       # FastAPI: domínio, parser, serviços e controllers
├── frontend/          # Flask: templates, proxy e Chart.js
└── run_app.bash       # inicializa os dois servidores
```

## Executar

Na raiz do repositório:

```bash
cd grafico-APP
./run_app.bash
```

O comando cria `.venv` se necessário, instala as dependências e inicia:

- Frontend Flask: <http://127.0.0.1:5000>
- Backend FastAPI: <http://127.0.0.1:8000/docs>

O frontend busca dados pelo proxy `/api/results`, que consulta o FastAPI. O
FastAPI lê o arquivo Hadoop configurado por `RESULT_FILE`.

```bash
RESULT_FILE=/caminho/para/part-r-00000 ./run_app.bash
```

Portas opcionais:

```bash
BACKEND_PORT=8100 FRONTEND_PORT=5100 ./run_app.bash
```

O dashboard utiliza Chart.js no template Flask, com tema escuro. A curva do
cotovelo usa a inércia (WCSS) calculada pelo reducer para cada `K`; o ponto de
cotovelo é uma interpretação visual, não uma decisão automática do sistema.
Valores `indefinida` são representados como `null` pela API e aparecem como
lacunas no gráfico.

## Testes

```bash
cd grafico-APP
source .venv/bin/activate
PYTHONPATH=backend/api:frontend pytest -q
```
