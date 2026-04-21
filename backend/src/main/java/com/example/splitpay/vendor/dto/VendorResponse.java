package com.example.splitpay.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorResponse {
    private UUID id;
    private String name;
    private String email;
    private String razorpayAccountId;
    private LocalDateTime createdAt;
}
