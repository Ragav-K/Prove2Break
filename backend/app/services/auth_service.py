"""Business logic for registration/login/refresh.

Never reveals whether an email exists on login failure (docs/AUTHENTICATION.md,
docs/SECURITY.md) — both "no such user" and "wrong password" surface as the
same InvalidCredentialsError -> generic 401.
"""
import uuid

from sqlalchemy.orm import Session

from app.auth.jwt import (
    TokenType,
    access_token_expires_in_seconds,
    create_access_token,
    create_refresh_token,
    decode_token,
)
from app.auth.password import hash_password, verify_password
from app.core.exceptions import EmailAlreadyRegisteredError, InvalidCredentialsError, InvalidTokenError
from app.models.user import User
from app.repositories.user_repository import UserRepository


class AuthService:
    def __init__(self, db: Session):
        self.db = db
        self.users = UserRepository(db)

    def register(self, email: str, password: str) -> User:
        existing = self.users.get_by_email(email)
        if existing is not None:
            raise EmailAlreadyRegisteredError("Email already registered")
        password_hash = hash_password(password)
        return self.users.create(email=email, password_hash=password_hash)

    def login(self, email: str, password: str) -> dict:
        user = self.users.get_by_email(email)
        if user is None or not verify_password(password, user.password_hash):
            # Same error regardless of which check failed -> no user enumeration.
            raise InvalidCredentialsError("Invalid email or password")

        access_token = create_access_token(user.id)
        refresh_token = create_refresh_token(user.id)
        return {
            "access_token": access_token,
            "refresh_token": refresh_token,
            "token_type": "bearer",
            "expires_in": access_token_expires_in_seconds(),
        }

    def refresh(self, refresh_token: str) -> dict:
        try:
            payload = decode_token(refresh_token, TokenType.REFRESH)
            user_id = uuid.UUID(payload["sub"])
        except (InvalidTokenError, ValueError, KeyError):
            raise InvalidTokenError("Invalid or expired refresh token")

        user = self.users.get_by_id(user_id)
        if user is None:
            raise InvalidTokenError("Invalid or expired refresh token")

        access_token = create_access_token(user.id)
        return {
            "access_token": access_token,
            "token_type": "bearer",
            "expires_in": access_token_expires_in_seconds(),
        }
