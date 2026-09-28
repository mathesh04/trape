package com.trape.backend.inventory.exception;

import com.trape.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OutOfStockException extends ApiException {
    public OutOfStockException(UUID variantId) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "OUT_OF_STOCK", "Variant " + variantId + " does not have enough stock");
    }

    public OutOfStockException(String label) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "OUT_OF_STOCK", label + " is out of stock");
    }
}
