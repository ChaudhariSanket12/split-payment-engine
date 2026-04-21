package com.example.splitpay.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received webhook. Signature present: {}", signature != null);

        if (signature == null || signature.isBlank()) {
            log.warn("Missing X-Razorpay-Signature header");
            return ResponseEntity.badRequest().body("Missing signature header");
        }

        if (!webhookService.verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        webhookService.handleEvent(payload);
        return ResponseEntity.ok("Webhook processed");
    }
}
