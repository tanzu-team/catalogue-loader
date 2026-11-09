package dev.cloudnative.retail.catalogue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productId;
    private String title;
    private Double price;
    private Integer stock;
    private String description;
    private String brand;
    private String category;

    /**
     * Pipe-delimited category segments for multi-level tagging in Valkey.
     */
    private String categories;

    /**
     * Splits hierarchical category "L1 > L2 > L3" into individual segments.
     */
    public List<String> getCategorySegments() {
        if (category == null || category.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(category.split(">"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String getCategoriesTagValue() {
        List<String> segments = getCategorySegments();
        return segments.isEmpty() ? (category != null ? category.trim() : "") : String.join("|", segments);
    }

    public boolean isInStock() {
        return stock != null && stock > 0;
    }

    // Backward compatibility aliases
    public String getProductName() {
        return title;
    }
    public void setProductName(String name) {
        this.title = name;
    }
    public String getProductBrand() {
        return brand;
    }
    public void setProductBrand(String brand) {
        this.brand = brand;
    }
    public String getGender() {
        return category;
    }
    public void setGender(String gender) {
        this.category = gender;
    }

    /**
     * Field used for generating embeddings.
     * Concatenation of title, category hierarchy, brand, and description.
     */
    private String embeddableText;
}
