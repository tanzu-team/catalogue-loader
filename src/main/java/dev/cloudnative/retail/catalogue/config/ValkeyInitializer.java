package dev.cloudnative.retail.catalogue.config;

import dev.cloudnative.retail.catalogue.service.ValkeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValkeyInitializer implements CommandLineRunner {

    private final ValkeyService valkeyService;

    @Override
    public void run(String... args) {
        log.info("Initializing Valkey Index...");
        try {
            valkeyService.createIndex();
            log.info("Valkey Index initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Valkey index: {}", e.getMessage());
        }
    }
}
