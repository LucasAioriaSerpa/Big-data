from flask import Flask

from frontend.app.config import FrontendSettings


def create_app(settings: FrontendSettings | None = None) -> Flask:
    app = Flask(
        __name__,
        template_folder="../templates",
        static_folder="../static",
    )
    app.config["RESULT_API_URL"] = (settings or FrontendSettings.from_environment()).api_url
    app.register_blueprint(results_blueprint)
    return app


from frontend.app.routes import results_blueprint  # noqa: E402
