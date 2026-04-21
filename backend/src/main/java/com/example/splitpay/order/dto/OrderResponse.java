package com.example.splitpay.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private String razorpayOrderId;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal vendorAmount;
    private String status;
    private String customerEmail;
    private UUID vendorId;
    private String vendorName;
    private String razorpayKeyId;
    private LocalDateTime createdAt;
}
