from flask import Blueprint, current_app, jsonify, render_template, request

from frontend.app.client import BackendApiError, ResultsApiClient

results_blueprint = Blueprint("results", __name__)


def _client() -> ResultsApiClient:
    return ResultsApiClient(current_app.config["RESULT_API_URL"])


@results_blueprint.get("/")
def dashboard():
    return render_template("dashboard.html")


@results_blueprint.get("/api/results")
def proxy_results():
    raw_k = request.args.get("k")
    try:
        k = int(raw_k) if raw_k else None
        if k is not None and k < 2:
            raise ValueError
        return jsonify(_client().get_results(k=k))
    except ValueError as exc:
        return jsonify({"error": "O parâmetro k deve ser um inteiro maior ou igual a 2."}), 400
    except BackendApiError as exc:
        return jsonify({"error": str(exc)}), 503


@results_blueprint.get("/api/elbow")
def proxy_elbow():
    try:
        return jsonify(_client().get_elbow())
    except BackendApiError as exc:
        return jsonify({"error": str(exc)}), 503
