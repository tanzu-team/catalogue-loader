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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CsvProductParser {

    public List<Product> parse(MultipartFile file) throws Exception {
        List<Product> products = new ArrayList<>();
        
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreHeaderCase(true)
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, format)) {

             for (CSVRecord record : csvParser) {
                 try {
                     Product product = Product.builder()
                             .productId(record.get("ProductID"))
                             .productName(record.get("ProductName"))
                             .productBrand(record.get("ProductBrand"))
                             .gender(record.get("Gender"))
                             .price(parsePrice(record.get("Price (INR)")))
                             .numImages(parseInteger(record.get("NumImages")))
                             .description(record.get("Description"))
                             .primaryColor(record.get("PrimaryColor"))
                             .build();
                     
                     // Create embeddable text: Name + Description
                     product.setEmbeddableText(
                         product.getProductName() + " " + product.getDescription()
                     );
                     
                     products.add(product);
                 } catch (Exception e) {
                     log.error("Error parsing row {}: {}", record.getRecordNumber(), e.getMessage());
                 }
             }
         }
         return products;
     }

    private Double parsePrice(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}