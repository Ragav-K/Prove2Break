"""JWT creation/verification for access + refresh tokens.

Algorithm/expiry are env-driven (JWT_ALGORITHM, JWT_ACCESS_TOKEN_EXPIRE_MINUTES,
JWT_REFRESH_TOKEN_EXPIRE_DAYS) per docs/AUTHENTICATION.md.
"""
import uuid
from datetime import datetime, timedelta, timezone
from enum import Enum
from typing import Any, Dict

import jwt
from jwt import ExpiredSignatureError, InvalidTokenError as PyJWTInvalidTokenError

from app.core.config import get_settings
from app.core.exceptions import InvalidTokenError

settings = get_settings()


class TokenType(str, Enum):
    ACCESS = "access"
    REFRESH = "refresh"


def _create_token(user_id: uuid.UUID, token_type: TokenType, expires_delta: timedelta) -> str:
    now = datetime.now(timezone.utc)
    payload: Dict[str, Any] = {
        "sub": str(user_id),
        "type": token_type.value,
        "iat": now,
        "exp": now + expires_delta,
        "jti": str(uuid.uuid4()),
    }
    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def create_access_token(user_id: uuid.UUID) -> str:
    return _create_token(
        user_id, TokenType.ACCESS, timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES)
    )


def create_refresh_token(user_id: uuid.UUID) -> str:
    return _create_token(
        user_id, TokenType.REFRESH, timedelta(days=settings.JWT_REFRESH_TOKEN_EXPIRE_DAYS)
    )


def access_token_expires_in_seconds() -> int:
    return settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES * 60


def decode_token(token: str, expected_type: TokenType) -> Dict[str, Any]:
    """Decode and validate a token, raising InvalidTokenError on any problem
    (expired, malformed, wrong signature, or wrong token type)."""
    try:
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
    except (ExpiredSignatureError, PyJWTInvalidTokenError):
        raise InvalidTokenError("Invalid or expired token")

    if payload.get("type") != expected_type.value:
        raise InvalidTokenError("Invalid token type")

    return payload
