package dev.cloudnative.retail.catalogue.controller;

import dev.cloudnative.retail.catalogue.model.Product;
import dev.cloudnative.retail.catalogue.model.UploadResponse;
import dev.cloudnative.retail.catalogue.service.CsvProductParser;
import dev.cloudnative.retail.catalogue.service.EmbeddingService;
import dev.cloudnative.retail.catalogue.service.ValkeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class CatalogueLoaderController {

    private final CsvProductParser csvParser;
    private final EmbeddingService embeddingService;
    private final ValkeyService valkeyService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(new UploadResponse(0, 0, 0, "No files provided"));
        }

        log.info("Received {} file(s) for upload", files.size());

        int total = 0;
        int success = 0;
        int failed = 0;

        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }
                log.info("Processing file: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

                // 1. Parse CSV
                List<Product> products = csvParser.parse(file);
                total += products.size();

                // 2. Process products
                for (Product product : products) {
                    try {
                        // Generate embedding from title + category hierarchy + brand + description
                        List<float[]> embeddings = embeddingService.generateEmbeddings(List.of(product.getEmbeddableText()));
                        if (embeddings.isEmpty()) {
                            throw new RuntimeException("No embedding generated");
                        }
                        float[] vector = embeddings.get(0);

                        // Prepare metadata for Valkey
                        Map<String, String> metadata = createMetadata(product);

                        // 3. Upsert to Valkey
                        valkeyService.upsertProduct(product.getProductId(), vector, metadata);

                        success++;
                    } catch (Exception e) {
                        log.error("Failed to process product {}: {}", product.getProductId(), e.getMessage());
                        failed++;
                    }
                }
            }

            return ResponseEntity.ok(new UploadResponse(total, success, failed, "Processing completed."));

        } catch (Exception e) {
            log.error("Error during upload processing", e);
            return ResponseEntity.internalServerError().body(new UploadResponse(total, success, failed, "Error: " + e.getMessage()));
        }
    }

    private static @NonNull Map<String, String> createMetadata(Product product) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", product.getTitle() != null ? product.getTitle() : "");
        metadata.put("name", product.getTitle() != null ? product.getTitle() : "");
        metadata.put("price", String.valueOf(product.getPrice() != null ? product.getPrice() : 0.0));
        metadata.put("stock", String.valueOf(product.getStock() != null ? product.getStock() : 10));
        metadata.put("description", product.getDescription() != null ? product.getDescription() : "");
        metadata.put("brand", product.getBrand() != null ? product.getBrand() : "General");
        metadata.put("category", product.getCategory() != null ? product.getCategory() : "General");
        metadata.put("categories", product.getCategories() != null ? product.getCategories() : product.getCategory());
        metadata.put("gender", product.getCategory() != null ? product.getCategory() : "General");
        return metadata;
    }
}
