package com.example.splitpay.webhook;

import com.example.splitpay.config.RazorpayConfig;
import com.example.splitpay.order.OrderRepository;
import com.example.splitpay.order.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final OrderRepository orderRepository;
    private final RazorpayConfig razorpayConfig;

    public boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void handleEvent(String payload) {
        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event");
        log.info("Received Razorpay webhook event: {}", eventType);

        switch (eventType) {
            case "order.paid" -> handleOrderPaid(event);
            case "payment.failed" -> handlePaymentFailed(event);
            default -> log.debug("Unhandled webhook event: {}", eventType);
        }
    }

    private void handleOrderPaid(JSONObject event) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject orderEntity = payload.getJSONObject("order").getJSONObject("entity");
            String razorpayOrderId = orderEntity.getString("id");

            Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(razorpayOrderId);
            if (orderOpt.isEmpty()) {
                log.warn("No order found for Razorpay order ID: {}", razorpayOrderId);
                return;
            }

            Order order = orderOpt.get();
            if ("PAID".equals(order.getStatus())) {
                log.info("Order {} already marked as PAID. Skipping.", order.getId());
                return;
            }

            order.setStatus("PAID");
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order {} marked as PAID at {}", order.getId(), order.getPaidAt());

        } catch (Exception e) {
            log.error("Error processing order.paid event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order.paid event: " + e.getMessage());
        }
    }

    private void handlePaymentFailed(JSONObject event) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject paymentEntity = payload.getJSONObject("payment").getJSONObject("entity");
            String razorpayOrderId = paymentEntity.optString("order_id");

            orderRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(order -> {
                order.setStatus("FAILED");
                orderRepository.save(order);
                log.warn("Order {} marked as FAILED", order.getId());
            });

        } catch (Exception e) {
            log.error("Error processing payment.failed event: {}", e.getMessage(), e);
        }
    }
}
