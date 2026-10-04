"""Domain-level exceptions raised by services, translated to HTTP responses in api/."""


class DomainError(Exception):
    """Base class for business-rule errors raised by the service layer."""


class EmailAlreadyRegisteredError(DomainError):
    pass


class InvalidCredentialsError(DomainError):
    pass


class InvalidTokenError(DomainError):
    pass


class NotFoundError(DomainError):
    """Raised for not-found-or-not-owned resources — maps to HTTP 404."""


class InvalidFileError(DomainError):
    """Raised for uploads that fail validation (wrong type, too large, unreadable) — maps to HTTP 422."""


class BadRequestError(DomainError):
    """Raised for malformed/inconsistent requests — maps to HTTP 400."""
