# Snip – URL Shortener

A full-stack URL shortener built with **Java Spring Boot** (backend) and **React + TypeScript** (frontend), containerized with Docker.

The application allows users to create shortened URLs, optionally choose custom aliases, view existing shortened URLs, page through results, and delete URLs.

---

## Features

- Create shortened URLs from long destination URLs
- Optional custom aliases
- Automatic random alias generation when no custom alias is provided
- Redirect short URLs to their original destination
- Display shortened URLs in the UI
- Delete shortened URLs
- Retrieve URLs with pagination
- Sort URLs by creation date, newest first
- Persist data using SQLite
- Run the full application locally with Docker Compose

## Architecture

```
frontend/      React + TypeScript (Vite)
backend/       Java Spring Boot Web API + SQLite
docker-compose.yml       Orchestrates both services
```

The frontend proxies API calls through Nginx in production, so there are no CORS concerns once containerized. In development, Vite's dev server proxies to the local API.

---

## Running the Application with Docker

From the project root, run:
```
docker-compose up --build -d
```

Once the containers are running, open:
```
Frontend: http://localhost:3000
Backend: http://localhost:8080
```

To stop the application:
```
docker-compose down
```

---

## API Reference

The API exposes the same contract expected by the existing frontend.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/shorten` | Create a shortened URL |
| `GET` | `/urls` | List all shortened URLs |
| `GET` | `/{alias}` | Redirect to the full URL |
| `DELETE` | `/{alias}` | Delete a shortened URL |

### POST /shorten

```json
// Request
{
  "fullUrl": "https://example.com/very/long/path",
  "customAlias": "my-alias"   // optional
}

// Response 201
{
  "alias": "my-alias",
  "fullUrl": "https://example.com/very/long/path",
  "shortUrl": "http://localhost:8080/my-alias"
}
```

Returns `400` if the URL is invalid, the alias is malformed, or the alias is already taken.

### GET /urls
If you require paging please use: GET /urls?page=0&size=5

```json
// Response 200
[
  {
    "alias": "my-alias",
    "fullUrl": "https://example.com/very/long/path",
    "shortUrl": "http://localhost:8080/my-alias"
  }
]
```

### GET /{alias}

Returns `302 Redirect` to the full URL, or `404` if the alias doesn't exist.

### DELETE /{alias}

Returns `204 No Content` on success, `404` if alias not found.

---

## Design Decisions & Assumptions

**Persistence** - SQLite via JDBC. The database file path is configurable via the `DATABASE_PATH` environment variable and is volume-mounted in Docker so data survives container restarts.

**Alias validation** - Aliases must be 2-64 characters, containing only letters, numbers, and hyphens. This mirrors typical URL shortener conventions and avoids characters that are awkward in URLs.

**Random alias generation** - Random alias generation is seven characters long, using upper and lower case letters and numbers.

**Error handling** - Invalid input and domain errors are translated into JSON error responses like `{ "error": "..." }`.

**Testing** - The backend includes service-level tests and can be extended with integration tests. The frontend includes React component tests and service-level API tests.

**Frontend** - The hosted frontend uses Nginx to proxy `/shorten`, `/urls`, and alias requests to the API, so the same Docker setup works without CORS issues.

## Production considerations

For a production deployment I would additionally:
- run dependency vulnerability scanning as part of CI.
- upgrade or mitigate any flagged dependencies.
- move persistence configuration out of local SQLite defaults.
- add authentication/authorization if URL management is not intended to be public.
- add rate limiting to prevent abuse.
- add logging and monitoring.
- dependant on the volume of traffic, make the random alias generation more efficient.
- setting up configuration for different environments so you can switch between dev, staging, and production.