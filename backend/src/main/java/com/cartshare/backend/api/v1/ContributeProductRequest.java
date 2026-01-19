package com.cartshare.backend.api.v1;

import lombok.Getter;
import lombok.Setter;

public class ContributeProductRequest {

    private static final String DEFAULT_CATEGORY_ID = "OUTROS";

    @Getter
    @Setter
    private String productName;
    private String categoryId; // Optional - null defaults to OUTROS

    public ContributeProductRequest() {
        this.categoryId = DEFAULT_CATEGORY_ID;
    }

    public ContributeProductRequest(String productName, String categoryId) {
        this.productName = productName;
        this.categoryId = (categoryId == null || categoryId.isBlank())
                ? DEFAULT_CATEGORY_ID
                : categoryId;
    }

    /**
     * Always returns a non-null categoryId.
     */
    public String getCategoryId() {
        return (categoryId == null || categoryId.isBlank())
                ? DEFAULT_CATEGORY_ID
                : categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = (categoryId == null || categoryId.isBlank())
                ? DEFAULT_CATEGORY_ID
                : categoryId;
    }
}