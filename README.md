# Spring Boot Catalogue Loader

A Spring Boot application to upload CSV product catalogue data, generate vector embeddings using an OpenAI-compatible embedding endpoint (e.g., LM Studio or OpenAI via Spring AI), and store products and embeddings in **Valkey** using an HNSW vector index.

## Features

- **CSV Product Parsing**: Parses product catalogue data (e.g., product name, brand, gender, price, description, primary color).
- **Spring AI Embeddings**: Generates embeddings via Spring AI using any OpenAI-compatible provider (configured for LM Studio running `text-embedding-nomic-embed-text-v1.5`).
- **Valkey Vector Indexing**: Automatically creates an HNSW vector index (`product_index`) on application startup and upserts product records with vector embeddings and metadata.
- **Multipart Upload**: Configured for large file uploads up to 15MB per file (90MB per request).

## Prerequisites

- **Java 17+**
- **Maven 3.8+** (or use the included `./mvnw`)
- **Embedding Provider**: OpenAI-compatible server (e.g., [LM Studio](https://lmstudio.ai/) running `text-embedding-nomic-embed-text-v1.5` on port `1234`)
- **Valkey**: Running instance with Search module enabled (see [Valkey Getting Started](#valkey-getting-started))

---

## Configuration

The application is configured in `src/main/resources/application.properties`:

```properties
spring.application.name=catalogue-loader

# OpenAI-compatible Embedding Config (e.g., LM Studio)
spring.ai.openai.base-url=http://localhost:1234/v1
spring.ai.openai.api-key=lm-studio
spring.ai.openai.embedding.model=text-embedding-nomic-embed-text-v1.5

# Valkey / Redis Config
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Multipart Upload Limits
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=90MB

# Application Indexing Config
app.loader.index-name=product_index
app.loader.dimension=768
```

---

## Build and Run

### 1. Build the Application
```bash
./mvnw clean package -DskipTests
```

### 2. Run Locally
```bash
./mvnw spring-boot:run
```
Or run the packaged JAR:
```bash
java -jar target/catalogue-loader-0.0.1-SNAPSHOT.jar
```

### 3. Run with Docker Compose (Valkey + Application)

To build and run both Valkey and the `catalogue-loader` application in Docker containers:

```bash
docker compose up --build -d
```

View application logs:
```bash
docker compose logs -f catalogue-loader
```

Stop services:
```bash
docker compose down
```

---

## API Usage

### Upload Product Catalogue

Upload a CSV file containing catalogue products (sample file available at `data/myntra_products_catalog.csv`):

- **Endpoint**: `POST /api/v1/products/upload`
- **Content-Type**: `multipart/form-data`
- **Form Field**: `file`

#### Example using `curl`:
```bash
curl -X POST "http://localhost:8080/api/v1/products/upload" \
  -F "file=@data/myntra_products_catalog.csv"
```

#### Example using IntelliJ / VS Code HTTP Client (`data/upload.http`):
```http
POST http://localhost:8080/api/v1/products/upload
Content-Type: multipart/form-data; boundary=boundary

--boundary
Content-Disposition: form-data; name="file"; filename="myntra_products_catalog.csv"

< ./myntra_products_catalog.csv
--boundary--
```

#### Response:
```json
{
  "total": 1000,
  "successful": 1000,
  "failed": 0,
  "message": "Processing completed."
}
```

---

## Valkey Getting Started

[Valkey](https://valkey.io/) is an open-source, high-performance key-value datastore stewarded by the Linux Foundation. To support vector search and HNSW indexing used by this application (`FT.CREATE`), Valkey must be run with the **Valkey Search** module.

### 1. Run Valkey with Search Module (Docker)

Start a local Valkey Search container:

```bash
docker run -d \
  --name valkey-bundle \
  -p 6379:6379 \
  valkey/valkey-bundle:latest
```

To stop or restart the container:
```bash
# Stop container
docker stop valkey-bundle

# Start stopped container
docker start valkey-bundle
```

### 2. Connect with CLI

Connect to Valkey using `valkey-cli` or `redis-cli`:

```bash
# Using docker exec
docker exec -it valkey-bundle valkey-cli

# Or directly from your host if redis-cli / valkey-cli is installed
redis-cli -p 6379
```

Test the connection:
```text
127.0.0.1:6379> PING
PONG
```

### 3. Verify Vector Index and Data

Once the `catalogue-loader` application starts and processes data, you can inspect the index and records directly in Valkey:

#### List Indexes:
```text
127.0.0.1:6379> FT._LIST
1) "product_index"
```

#### Inspect Index Details:
```text
127.0.0.1:6379> FT.INFO product_index
```

#### View Stored Product Keys:
```text
127.0.0.1:6379> KEYS product:*
```

#### Inspect a Product Record:
```text
127.0.0.1:6379> HGETALL product:10017413
```

### Hybrid Search (Text Filter + Vector Similarity)
Find products that are semantically similar to a query vector, but strictly filtered by attributes (e.g. only under a certain price or within a specific gender/brand):
```text
127.0.0.1:6379> FT.SEARCH product_index "(@gender:{Men} @price:[0 1000])=>[KNN 5 @vector $vec AS score]" PARAMS 2 vec "<raw_vector_bytes>" SORTBY score DIALECT 2
```

### Full-Text Search with Prefix & Fuzzy Matching
```text
# Prefix match (all products starting with "denim")
127.0.0.1:6379> FT.SEARCH product_index "denim*" RETURN 2 name price LIMIT 0 5

# Text search with price range filter
127.0.0.1:6379> FT.SEARCH product_index "jacket @price:[1000 3000]" RETURN 2 name price
```

### Tag Aggregations / Faceting
Inspect top brands or categories:
```text
127.0.0.1:6379> FT.AGGREGATE product_index "*" GROUPBY 1 @brand REDUCE COUNT 0 AS num_products SORTBY 2 @num_products DESC LIMIT 0 10
```
