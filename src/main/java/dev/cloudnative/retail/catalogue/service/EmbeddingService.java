package dev.cloudnative.retail.catalogue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public List<float[]> generateEmbeddings(List<String> texts) {
        log.info("Generating embeddings for {} texts", texts.size());
        try {
            return embeddingModel.embed(texts);
        } catch (Exception e) {
            log.error("Failed to generate embeddings for input: {}. Error: {}", texts, e.getMessage());
            return List.of(); // Return empty list instead of throwing exception
        }
    }

    public List<float[]> generateEmbeddingsInBatches(List<String> texts, int batchSize) {
        log.info("Generating embeddings for {} texts in batches of {}", texts.size(), batchSize);
        return IntStream.range(0, (texts.size() + batchSize - 1) / batchSize)
                .mapToObj(i -> texts.subList(i * batchSize, Math.min((i + 1) * batchSize, texts.size())))
                .flatMap(batch -> {
                    try {
                        return embeddingModel.embed(batch).stream();
                    } catch (Exception e) {
                        log.error("Failed to generate embeddings for batch: {}. Error: {}", batch, e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }
}
