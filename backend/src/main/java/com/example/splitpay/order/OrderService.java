package com.example.splitpay.order;

import com.example.splitpay.config.RazorpayConfig;
import com.example.splitpay.order.dto.CreateOrderRequest;
import com.example.splitpay.order.dto.OrderResponse;
import com.example.splitpay.order.model.Order;
import com.example.splitpay.vendor.VendorRepository;
import com.example.splitpay.vendor.model.Vendor;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.20");

    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + request.getVendorId()));

        BigDecimal totalAmount = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);

        // Convert to paise (Razorpay uses smallest currency unit)
        long totalAmountPaise = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        String razorpayOrderId = createRazorpayOrder(totalAmountPaise);

        Order order = Order.builder()
                .vendor(vendor)
                .customerEmail(request.getCustomerEmail())
                .amount(totalAmount)
                .platformFee(platformFee)
                .razorpayOrderId(razorpayOrderId)
                .status("PENDING")
                .build();

        order = orderRepository.save(order);
        log.info("Created order {} with Razorpay order ID: {}", order.getId(), razorpayOrderId);

        return toResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return toResponse(order);
    }

    private String createRazorpayOrder(long totalAmountPaise) {
        try {
            // Build basic Razorpay order request (test keys compatible)
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", totalAmountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            return razorpayOrder.get("id");

        } catch (Exception e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }

    private OrderResponse toResponse(Order order) {
        BigDecimal vendorAmount = order.getAmount().subtract(order.getPlatformFee());
        return OrderResponse.builder()
                .id(order.getId())
                .razorpayOrderId(order.getRazorpayOrderId())
                .amount(order.getAmount())
                .platformFee(order.getPlatformFee())
                .vendorAmount(vendorAmount)
                .status(order.getStatus())
                .customerEmail(order.getCustomerEmail())
                .vendorId(order.getVendor().getId())
                .vendorName(order.getVendor().getName())
                .razorpayKeyId(razorpayConfig.getKeyId())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
