package com.example.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response object")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "500")
    private int status;

    @Schema(description = "Error type", example = "Internal Server Error")
    private String error;

    @Schema(description = "Error message", example = "Payment processing failed")
    private String message;

    @Schema(description = "Request path", example = "/api/orders/123/process-payment")
    private String path;

    @Schema(description = "Error code for client handling", example = "PAYMENT_SERVICE_ERROR")
    private String errorCode;
}