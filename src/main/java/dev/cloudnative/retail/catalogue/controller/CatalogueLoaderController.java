package dev.cloudnative.retail.catalogue.controller;

import dev.cloudnative.retail.catalogue.model.Product;
import dev.cloudnative.retail.catalogue.model.UploadResponse;
import dev.cloudnative.retail.catalogue.service.CsvProductParser;
import dev.cloudnative.retail.catalogue.service.EmbeddingService;
import dev.cloudnative.retail.catalogue.service.ValkeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("Received file upload: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new UploadResponse(0, 0, 0, "File is empty"));
        }

        try {
            // 1. Parse
            List<Product> products = csvParser.parse(file);
            int total = products.size();
            int success = 0;
            int failed = 0;

            // 2. Process
            for (Product product : products) {
                try {
                    // Generate embedding
                    List<float[]> embeddings = embeddingService.generateEmbeddings(List.of(product.getEmbeddableText()));
                    if (embeddings.isEmpty()) {
                        throw new RuntimeException("No embedding generated");
                    }
                    float[] vector = embeddings.get(0);

                    // Prepare metadata
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("name", product.getProductName());
                    metadata.put("brand", product.getProductBrand());
                    metadata.put("gender", product.getGender());
                    metadata.put("price", String.valueOf(product.getPrice()));
                    metadata.put("description", product.getDescription());
                    metadata.put("color", product.getPrimaryColor());

                    // 3. Upsert to Valkey
                    valkeyService.upsertProduct(product.getProductId(), vector, metadata);
                    
                    success++;
                } catch (Exception e) {
                    log.error("Failed to process product {}: {}", product.getProductId(), e.getMessage());
                    failed++;
                }
            }

            return ResponseEntity.ok(new UploadResponse(total, success, failed, "Processing completed."));

        } catch (Exception e) {
            log.error("Error during upload processing", e);
            return ResponseEntity.internalServerError().body(new UploadResponse(0, 0, 0, "Error: " + e.getMessage()));
        }
    }
}
