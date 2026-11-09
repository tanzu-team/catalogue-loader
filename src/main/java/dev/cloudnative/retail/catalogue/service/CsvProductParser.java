package dev.cloudnative.retail.catalogue.service;

import dev.cloudnative.retail.catalogue.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CsvProductParser {

    public List<Product> parse(MultipartFile file) throws Exception {
        List<Product> products = new ArrayList<>();

        // Detect delimiter: check first line for tabs vs commas
        char delimiter = ',';
        byte[] fileBytes = file.getBytes();
        String contentSample = new String(fileBytes, 0, Math.min(fileBytes.length, 2048), StandardCharsets.UTF_8);
        String firstLine = contentSample.lines().findFirst().orElse("");
        if (firstLine.contains("\t") && !firstLine.contains(",")) {
            delimiter = '\t';
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, format)) {

            for (CSVRecord record : csvParser) {
                try {
                    String id = getField(record, "id", "ProductID", "product_id");
                    String title = getField(record, "title", "ProductName", "name");
                    Double price = parsePrice(getField(record, "price", "Price (INR)", "Price"));
                    Integer stock = parseInteger(getField(record, "stock", "Stock", "inventory", "quantity"));
                    String description = getField(record, "description", "Description");
                    String brand = getField(record, "brand", "ProductBrand");
                    String category = getField(record, "category", "Gender");

                    if (id.isBlank()) {
                        continue;
                    }

                    Product product = Product.builder()
                            .productId(id)
                            .title(title)
                            .price(price)
                            .stock(stock)
                            .description(description)
                            .brand(!brand.isBlank() ? brand : "General")
                            .category(!category.isBlank() ? category : "General")
                            .build();

                    product.setCategories(product.getCategoriesTagValue());

                    // Create embeddable text: Title + Category hierarchy + Brand + Description
                    StringBuilder sb = new StringBuilder();
                    if (!title.isBlank()) sb.append(title).append(" ");
                    if (!category.isBlank()) sb.append(category).append(" ");
                    if (!brand.isBlank()) sb.append(brand).append(" ");
                    if (!description.isBlank()) sb.append(description);
                    product.setEmbeddableText(sb.toString().trim());

                    products.add(product);
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }
        }
        return products;
    }

    private String getField(CSVRecord record, String... headerNames) {
        for (String name : headerNames) {
            if (record.isMapped(name)) {
                String val = record.get(name);
                if (val != null) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    private Double parsePrice(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 10; // Default stock if not specified
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 10;
        }
    }
}
