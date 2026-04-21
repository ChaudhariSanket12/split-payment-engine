package com.example.splitpay.payment;

import com.example.splitpay.payment.dto.PaymentVerificationRequest;
import com.example.splitpay.payment.dto.PaymentVerificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        PaymentVerificationResponse response = paymentService.verifyPayment(request);
        HttpStatus status = response.isVerified() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}
