package com.example.splitpay.payment;

import com.example.splitpay.config.RazorpayConfig;
import com.example.splitpay.order.OrderRepository;
import com.example.splitpay.order.model.Order;
import com.example.splitpay.payment.dto.PaymentVerificationRequest;
import com.example.splitpay.payment.dto.PaymentVerificationResponse;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final RazorpayConfig razorpayConfig;

    @Transactional
    public PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) {
        Order order = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found for Razorpay order ID: " + request.getRazorpayOrderId()));

        boolean verified = verifySignature(request);

        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());

        if (verified) {
            order.setStatus("SUCCESS");
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Payment verified for order {}", order.getId());

            return PaymentVerificationResponse.builder()
                    .verified(true)
                    .status("SUCCESS")
                    .message("Payment signature verified successfully")
                    .razorpayOrderId(request.getRazorpayOrderId())
                    .razorpayPaymentId(request.getRazorpayPaymentId())
                    .build();
        }

        order.setStatus("FAILED");
        order.setPaidAt(null);
        orderRepository.save(order);
        log.warn("Payment signature verification failed for order {}", order.getId());

        return PaymentVerificationResponse.builder()
                .verified(false)
                .status("FAILED")
                .message("Invalid payment signature")
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .build();
    }

    private boolean verifySignature(PaymentVerificationRequest request) {
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", request.getRazorpayOrderId());
        attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
        attributes.put("razorpay_signature", request.getRazorpaySignature());

        try {
            return Utils.verifyPaymentSignature(attributes, razorpayConfig.getKeySecret());
        } catch (RazorpayException e) {
            log.error("Unable to verify payment signature: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to verify payment signature: " + e.getMessage(), e);
        }
    }
}
