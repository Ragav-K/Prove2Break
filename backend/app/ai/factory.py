"""Single place that decides which AIService implementation to hand out.

Business/service code should call `get_ai_service(settings)` and never
import `MockAIProvider`/`OpenAICompatibleProvider` directly — this keeps the
provider choice a config-only concern (Settings.AI_PROVIDER).
"""
from app.ai.providers import MockAIProvider, OpenAICompatibleProvider
from app.ai.service import AIService
from app.core.config import Settings


def get_ai_service(settings: Settings) -> AIService:
    provider = (settings.AI_PROVIDER or "mock").strip().lower()
    if provider in ("", "mock", "none"):
        return MockAIProvider()
    # Any configured real-provider name currently routes to the unimplemented
    # seam — see app/ai/providers.py::OpenAICompatibleProvider.
    return OpenAICompatibleProvider(api_key=settings.AI_API_KEY, model_name=settings.AI_MODEL_NAME)
