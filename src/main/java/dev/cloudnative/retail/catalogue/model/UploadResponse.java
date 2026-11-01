package dev.cloudnative.retail.catalogue.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResponse {
    private long totalRows;
    private long successfulRows;
    private long failedRows;
    private String message;
}
