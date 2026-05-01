# Media Service

File upload and storage service for the HiveMind platform. Handles media file uploads, storage management, and serving media content.

## Details

| Property | Value |
|----------|-------|
| **Port** | `8087` |
| **Database** | PostgreSQL |
| **Role** | File Upload + Storage |

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/*.jar

# Docker
docker build -t hivemind/media-service .
```

## Links

- [Main Repository](https://github.com/AhmedNijim92/hivemind-backend)
