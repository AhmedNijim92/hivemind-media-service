# Media Service

> HiveMind File Upload & Media Management Microservice

## Overview

The media-service handles file uploads (images, videos, documents), stores them on AWS S3, and manages metadata in PostgreSQL. It provides presigned URLs for secure, time-limited access and supports direct file downloads for local development.

## Service Info

| Property | Value |
|----------|-------|
| Port | 8087 |
| Service Name | `media-service` |
| Database | PostgreSQL |
| Storage | AWS S3 |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2023.0.3 |
| Java | 17 |

## Architecture

```
Client (via Gateway)
  │
  ▼
MediaController
  │
  ├── IMediaService (upload, getMediaById, generatePresignedUrl, getMediaByUploader, delete)
  │       ├── MediaFileRepository (JPA/PostgreSQL)
  │       └── S3Client (AWS SDK)
  │
  └── S3Config (S3Client + S3Presigner beans)
```

## API Endpoints

Base path: `/api/v1/media`
All endpoints require JWT except download.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/upload` | JWT | Upload a file (multipart) |
| GET | `/{mediaId}` | JWT | Get media metadata |
| GET | `/{mediaId}/presigned-url` | JWT | Get presigned S3 download URL |
| GET | `/{mediaId}/download` | Public | Download file directly |
| GET | `/my` | JWT | Get current user's uploads |
| DELETE | `/{mediaId}` | JWT | Delete media file |

### Request/Response Examples

#### POST /api/v1/media/upload
```
Content-Type: multipart/form-data

file: (binary)
referenceId: uuid (optional)
referenceType: POST | GROUP | USER_AVATAR (default: POST)
```

```json
// Response (201)
{
  "mediaId": "uuid",
  "uploaderId": "uuid",
  "originalFilename": "photo.jpg",
  "contentType": "image/jpeg",
  "fileSize": 245760,
  "referenceId": "uuid",
  "referenceType": "POST",
  "createdAt": "2025-06-04T10:30:00"
}
```

#### GET /api/v1/media/{mediaId}/presigned-url
```json
// Response (200)
"https://hivemind-media-dev.s3.amazonaws.com/uploads/uuid/photo.jpg?X-Amz-..."
```

## Data Model

### MediaFile (PostgreSQL table: `media_files`)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| media_id | UUID | PK, auto-gen | Media identifier |
| uploader_id | UUID | NOT NULL | Who uploaded |
| original_filename | String | NOT NULL | Original file name |
| s3_key | String | NOT NULL, UNIQUE | S3 object key |
| s3_bucket | String | NOT NULL | S3 bucket name |
| content_type | String | — | MIME type |
| file_size | long | — | File size in bytes |
| reference_id | UUID | — | Related entity (post, group, etc.) |
| reference_type | String | — | POST, GROUP, USER_AVATAR |
| created_at | LocalDateTime | — | Upload timestamp |

## S3 Storage

### Key Pattern
```
uploads/{uploaderId}/{uuid}-{originalFilename}
```

### Production Setup (AWS)
- Bucket: Private (no public access)
- Encryption: AES-256 server-side
- Versioning: Enabled
- Access: CloudFront OAI for reads, IRSA for writes
- Lifecycle: Move to STANDARD_IA after 90 days

### Local Development
- Files stored on local filesystem at `./media-uploads/`
- Download endpoint serves files directly from disk
- Presigned URLs will fail without real AWS credentials

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | PostgreSQL host |
| DB_PORT | 5432 | PostgreSQL port |
| DB_USERNAME | postgres | Database user |
| DB_PASSWORD | postgres | Database password |
| AWS_ACCESS_KEY | dev-key | AWS access key |
| AWS_SECRET_KEY | dev-secret | AWS secret key |
| AWS_REGION | us-east-1 | AWS region |
| S3_BUCKET | hivemind-media-dev | S3 bucket name |
| EUREKA_SERVER | http://localhost:8761/eureka | Eureka URL |

### File Upload Limits

- Max file size: 50MB
- Max request size: 50MB
- Gateway codec max in-memory: 50MB

## Dependencies

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- postgresql (driver)
- spring-boot-starter-validation
- spring-boot-starter-actuator
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-config
- software.amazon.awssdk:s3 (2.20.0)
- hivemind-common (1.0.0)
- lombok

## Running Locally

```bash
# Prerequisites: PostgreSQL running on port 5432 (database: media_db)
cd microservices/media-service
mvn spring-boot:run
```

JPA auto-creates the `media_files` table via `ddl-auto: update`.

## Known Issues

1. AWS S3 credentials required for actual uploads — in dev mode, files are stored locally
2. No virus scanning on uploads
3. No image resizing/thumbnail generation
4. Presigned URLs have a fixed expiry (consider making configurable)
