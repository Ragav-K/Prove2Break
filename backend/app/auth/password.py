"""Password hashing.

Choice: argon2id via `argon2-cffi`, over bcrypt/passlib.
Rationale (see docs/AUTHENTICATION.md, which lists "bcrypt or argon2 — decided at
implementation, argon2id preferred where library support is convenient"):
argon2-cffi provides argon2id directly, is actively maintained, has no 72-byte
input truncation footgun (unlike bcrypt), and passlib's bcrypt backend has had
recent compatibility issues with newer bcrypt releases. argon2-cffi's
PasswordHasher already encodes a random salt + parameters into the stored hash
string, so no separate salt column is needed.
"""
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, VerificationError, InvalidHash

_hasher = PasswordHasher()


def hash_password(plain_password: str) -> str:
    return _hasher.hash(plain_password)


def verify_password(plain_password: str, password_hash: str) -> bool:
    try:
        return _hasher.verify(password_hash, plain_password)
    except (VerifyMismatchError, VerificationError, InvalidHash):
        return False
