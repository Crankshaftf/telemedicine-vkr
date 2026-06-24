FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends libpq-dev gcc \
    && rm -rf /var/lib/apt/lists/*

COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY backend/app ./app
COPY backend/scripts ./scripts
COPY admin-web /app/admin-web
COPY download-web /app/download-web

RUN mkdir -p uploads data downloads

ENV ADMIN_PANEL_DIR=/app/admin-web
ENV DOWNLOAD_DIR=/app/download-web

EXPOSE 8000

CMD ["sh", "-c", "python scripts/seed.py; uvicorn app.main:app --host 0.0.0.0 --port 8000"]
