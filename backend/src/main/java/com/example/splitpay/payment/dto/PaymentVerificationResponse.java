package com.example.splitpay.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentVerificationResponse {
    private boolean verified;
    private String status;
    private String message;
    private String razorpayOrderId;
    private String razorpayPaymentId;
}
