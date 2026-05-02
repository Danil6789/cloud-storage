# Cloud Storage

[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://adoptium.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-blue.svg)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-✔-blue.svg)](https://www.docker.com/)
[![Spring](https://img.shields.io/badge/Spring-6.1-green.svg)](https://spring.io/)

## Description

Cloud Storage is a web application that allows users to upload, download, organize, and share files and folders. Users can create a personal storage space, manage directory structures, upload multiple files at once, and download entire folders as ZIP archives. The application provides a REST API and a React-based frontend.

## Frontend REST API Interface

Frontend: [cloud-storage-frontend](https://github.com/zhukovsd/cloud-storage-frontend)

## Technologies

| Component | Technology |
|-----------|------------|
| **Backend** | Java 21, Spring Framework 7.0 (MVC, Security, Data JPA) |
| **Database** | PostgreSQL 15 |
| **Session Management** | Redis (Spring Session) |
| **Object Storage** | MinIO (S3-compatible API) |
| **Mapping** | MapStruct |
| **Database Migration** | Liquibase |
| **Frontend** | React (separate repository) |
| **Containerization** | Docker, Docker Compose |
| **Web Server** | Nginx (reverse proxy) |
| **Security** | Spring Security (session‑based), BCrypt |
| **File Handling** | Multipart upload, ZIP streaming |
| **Build Tool** | Gradle 8.5 |

## Deployment

### For VPS (Docker)

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Install Docker Compose plugin
apt install docker-compose-plugin -y

# Clone project
git clone https://github.com/Danil6789/cloud-storage.git
cd cloud-storage

# Create .env file (optional, override defaults)
cat > .env << EOF
DB_PASSWORD=admin
MINIO_SECRET_KEY=minioadmin123
ALLOWED_ORIGINS=http://95.170.124.179
EOF

# Build and run
docker compose up --build -d
```

# API Overview

All endpoints require session authentication (cookie `SESSION`). The API prefix is `/api`.

---

## 1. Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/sign-up` | Register a new user |
| `POST` | `/api/auth/sign-in` | Log in (creates session) |
| `POST` | `/api/auth/sign-out` | Log out |

**Request body** (`sign-up` / `sign-in`):

```json
{
  "username": "user",
  "password": "pass"
}
```

**Response** (`sign-up` / `sign-in`):

```json
{
  "id": 1,
  "username": "user"
}
```

---

## 2. File and Folder Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/resource?path={path}` | Get resource info (file or folder) |
| `DELETE` | `/api/resource?path={path}` | Delete a file or folder (recursively) |
| `PATCH` | `/api/resource/move?from={from}&to={to}` | Move or rename a resource |
| `GET` | `/api/resource/download?path={path}` | Download a file or folder (folder as ZIP) |
| `GET` | `/api/resource/search?query={query}` | Search files/folders by name |
| `POST` | `/api/resource?path={path}` | Upload files (multipart) |
| `GET` | `/api/directory?path={path}` | List contents of a folder (non-recursive) |
| `POST` | `/api/directory?path={path}` | Create an empty folder |

### Upload Requirements

- `Content-Type: multipart/form-data`
- Part name: `object`
- Multiple files allowed
- Filenames may contain sub-paths (e.g. `folder/file.txt`) — the server creates necessary parent directories

### Response (upload & listing)

```json
[
  {
    "path": "folder/",
    "name": "file.txt",
    "size": 123,
    "type": "FILE"
  },
  {
    "path": "",
    "name": "subfolder/",
    "size": null,
    "type": "DIRECTORY"
  }
]
```

---

## 3. User

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/user/me` | Get current authenticated user info |

**Response:**

```json
{
  "username": "user"
}
```

---

## Error Responses

| HTTP Status | Meaning |
|-------------|---------|
| `400` | Bad request (invalid path, missing parameters) |
| `401` | Unauthorized (not logged in) |
| `404` | Resource not found |
| `409` | Conflict (resource already exists) |
| `500` | Internal server error |

**Error body:**

```json
{
  "message": "Description of the error"
}
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring profile (`dev`, `prod`, `test`) |
| `DB_PASSWORD` | `admin` | PostgreSQL password |
| `MINIO_SECRET_KEY` | `minioadmin123` | MinIO secret key |
| `ALLOWED_ORIGINS` | `http://localhost:8080` | CORS allowed origins (comma-separated) |

---

## License

MIT


