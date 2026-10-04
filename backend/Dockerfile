# Prove2Break backend (FastAPI) — see docs/DEPLOYMENT.md.
#
# This container runs migrations are NOT executed here on purpose
# (docs/DEPLOYMENT.md: "migrations run as a pre-start step ... never implicitly
# inside the app's request-handling lifecycle") — run `alembic upgrade head`
# as a separate deploy step before starting this container against a real
# environment's database.
FROM python:3.11-slim

WORKDIR /app

# psycopg[binary] ships its own libpq, but build-essential covers any
# source-build fallback on platforms without a matching wheel.
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY migrations ./migrations
COPY alembic.ini .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
