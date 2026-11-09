package dev.cloudnative.retail.catalogue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValkeyService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.loader.index-name:product_index}")
    private String indexName;

    @Value("${app.loader.dimension:768}")
    private int dimension;

    /**
     * Creates a HNSW index in Valkey with multi-level category tags and numeric price/stock.
     */
    public void createIndex() {
        log.info("Creating Valkey index: {}", indexName);

        try {
            redisTemplate.execute((RedisConnection connection) -> {
                connection.commands().execute("FT.CREATE",
                        indexName.getBytes(StandardCharsets.UTF_8),
                        "ON".getBytes(StandardCharsets.UTF_8), "HASH".getBytes(StandardCharsets.UTF_8),
                        "PREFIX".getBytes(StandardCharsets.UTF_8), "1".getBytes(StandardCharsets.UTF_8), "product:".getBytes(StandardCharsets.UTF_8),
                        "SCHEMA".getBytes(StandardCharsets.UTF_8),
                        "id".getBytes(StandardCharsets.UTF_8), "TAG".getBytes(StandardCharsets.UTF_8),
                        "title".getBytes(StandardCharsets.UTF_8), "TEXT".getBytes(StandardCharsets.UTF_8),
                        "name".getBytes(StandardCharsets.UTF_8), "TEXT".getBytes(StandardCharsets.UTF_8),
                        "brand".getBytes(StandardCharsets.UTF_8), "TAG".getBytes(StandardCharsets.UTF_8),
                        "category".getBytes(StandardCharsets.UTF_8), "TAG".getBytes(StandardCharsets.UTF_8), "SEPARATOR".getBytes(StandardCharsets.UTF_8), "|".getBytes(StandardCharsets.UTF_8),
                        "categories".getBytes(StandardCharsets.UTF_8), "TAG".getBytes(StandardCharsets.UTF_8), "SEPARATOR".getBytes(StandardCharsets.UTF_8), "|".getBytes(StandardCharsets.UTF_8),
                        "gender".getBytes(StandardCharsets.UTF_8), "TAG".getBytes(StandardCharsets.UTF_8),
                        "price".getBytes(StandardCharsets.UTF_8), "NUMERIC".getBytes(StandardCharsets.UTF_8),
                        "stock".getBytes(StandardCharsets.UTF_8), "NUMERIC".getBytes(StandardCharsets.UTF_8),
                        "description".getBytes(StandardCharsets.UTF_8), "TEXT".getBytes(StandardCharsets.UTF_8),
                        "vector".getBytes(StandardCharsets.UTF_8), "VECTOR".getBytes(StandardCharsets.UTF_8), "HNSW".getBytes(StandardCharsets.UTF_8), "6".getBytes(StandardCharsets.UTF_8),
                        "TYPE".getBytes(StandardCharsets.UTF_8), "FLOAT32".getBytes(StandardCharsets.UTF_8),
                        "DIM".getBytes(StandardCharsets.UTF_8), String.valueOf(dimension).getBytes(StandardCharsets.UTF_8),
                        "DISTANCE_METRIC".getBytes(StandardCharsets.UTF_8), "COSINE".getBytes(StandardCharsets.UTF_8)
                );
                return null;
            });
            log.info("Index created successfully.");
        } catch (Exception e) {
            String msg = (e.getMessage() != null ? e.getMessage() : "")
                    + (e.getCause() != null && e.getCause().getMessage() != null ? " " + e.getCause().getMessage() : "");
            if (msg.toLowerCase().contains("already exists")) {
                log.info("Index already exists: {}", indexName);
            } else {
                log.error("Failed to create index: {}", e.getMessage(), e);
            }
        }
    }

    public void upsertProduct(String productId, float[] vector, Map<String, String> metadata) {
        String key = "product:" + productId;
        byte[] vectorBytes = floatArrayToBytes(vector);

        redisTemplate.execute((RedisConnection connection) -> {
            connection.commands().execute("HSET",
                    key.getBytes(StandardCharsets.UTF_8),
                    "id".getBytes(StandardCharsets.UTF_8), productId.getBytes(StandardCharsets.UTF_8),
                    "title".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("title", metadata.getOrDefault("name", "")).getBytes(StandardCharsets.UTF_8),
                    "name".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("title", metadata.getOrDefault("name", "")).getBytes(StandardCharsets.UTF_8),
                    "price".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("price", "0.0").getBytes(StandardCharsets.UTF_8),
                    "stock".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("stock", "10").getBytes(StandardCharsets.UTF_8),
                    "description".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("description", "").getBytes(StandardCharsets.UTF_8),
                    "brand".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("brand", "General").getBytes(StandardCharsets.UTF_8),
                    "category".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("category", metadata.getOrDefault("gender", "General")).getBytes(StandardCharsets.UTF_8),
                    "categories".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("categories", metadata.getOrDefault("category", "General")).getBytes(StandardCharsets.UTF_8),
                    "gender".getBytes(StandardCharsets.UTF_8), metadata.getOrDefault("category", metadata.getOrDefault("gender", "General")).getBytes(StandardCharsets.UTF_8),
                    "vector".getBytes(StandardCharsets.UTF_8), vectorBytes
            );
            return null;
        });
    }

    private byte[] floatArrayToBytes(float[] array) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(array.length * 4);
        bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (float f : array) {
            bb.putFloat(f);
        }
        return bb.array();
    }
}
