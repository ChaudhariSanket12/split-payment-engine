package com.example.splitpay.reconciliation;

import com.example.splitpay.order.OrderRepository;
import com.example.splitpay.order.model.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;

    public void runReconciliation() {
        log.info("=== Starting Reconciliation Job ===");

        reconcilePendingOrders();
        fetchAndLogSettlements();

        log.info("=== Reconciliation Job Completed ===");
    }

    private void reconcilePendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus("PENDING");
        log.info("Found {} PENDING orders to reconcile", pendingOrders.size());

        for (Order order : pendingOrders) {
            if (order.getRazorpayOrderId() == null) continue;
            try {
                com.razorpay.Order razorpayOrder = razorpayClient.orders.fetch(order.getRazorpayOrderId());
                String rzpStatus = razorpayOrder.get("status");
                log.info("Order [{}] local=PENDING, Razorpay status={}", order.getId(), rzpStatus);

                if ("paid".equalsIgnoreCase(rzpStatus)) {
                    log.warn("DISCREPANCY: Order {} is PAID on Razorpay but PENDING locally. Manual review needed.", order.getId());
                }
            } catch (Exception e) {
                log.error("Failed to fetch Razorpay order {} during reconciliation: {}", order.getRazorpayOrderId(), e.getMessage());
            }
        }
    }

    private void fetchAndLogSettlements() {
        try {
            log.info("Fetching recent settlements from Razorpay...");
            JSONObject params = new JSONObject();
            params.put("count", 10);

            // Settlements API - logs the response for audit purposes
            // In production, parse and store settlement data
            log.info("Settlement fetch simulated (enable Razorpay X account for full settlement data)");

            // DB-side summary
            long totalOrders = orderRepository.count();
            List<Order> paidOrders = orderRepository.findByStatus("PAID");
            BigDecimal totalRevenue = paidOrders.stream()
                    .map(Order::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPlatformFees = paidOrders.stream()
                    .map(Order::getPlatformFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("--- Reconciliation Summary ---");
            log.info("Total Orders in DB    : {}", totalOrders);
            log.info("Paid Orders           : {}", paidOrders.size());
            log.info("Total Revenue (INR)   : {}", totalRevenue);
            log.info("Platform Fees (INR)   : {}", totalPlatformFees);
            log.info("Vendor Payouts (INR)  : {}", totalRevenue.subtract(totalPlatformFees));

        } catch (Exception e) {
            log.error("Settlement reconciliation failed: {}", e.getMessage(), e);
        }
    }
}
