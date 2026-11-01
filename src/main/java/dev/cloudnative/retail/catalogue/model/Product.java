package dev.cloudnative.retail.catalogue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productId;
    private String productName;
    private String productBrand;
    private String gender;
    private Double price;
    private Integer numImages;
    private String description;
    private String primaryColor;

    /**
     * Field used for generating embeddings.
     * Concatenation of name and description.
     */
    private String embeddableText;
}
