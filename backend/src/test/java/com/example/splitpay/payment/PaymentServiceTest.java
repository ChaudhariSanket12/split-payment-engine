package com.example.splitpay.payment;

import com.example.splitpay.config.RazorpayConfig;
import com.example.splitpay.order.OrderRepository;
import com.example.splitpay.order.model.Order;
import com.example.splitpay.payment.dto.PaymentVerificationRequest;
import com.example.splitpay.payment.dto.PaymentVerificationResponse;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RazorpayConfig razorpayConfig;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void verifyPayment_validSignature_marksOrderSuccess() throws RazorpayException {
        String razorpayOrderId = "order_test_valid";
        String razorpayPaymentId = "pay_test_valid";
        String secret = "test_secret_key";
        String signature = Utils.getHash(razorpayOrderId + "|" + razorpayPaymentId, secret);

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .platformFee(new BigDecimal("200.00"))
                .razorpayOrderId(razorpayOrderId)
                .status("PENDING")
                .build();

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId(razorpayOrderId);
        request.setRazorpayPaymentId(razorpayPaymentId);
        request.setRazorpaySignature(signature);

        when(orderRepository.findByRazorpayOrderId(razorpayOrderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayConfig.getKeySecret()).thenReturn(secret);

        PaymentVerificationResponse response = paymentService.verifyPayment(request);

        assertTrue(response.isVerified());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(razorpayOrderId, response.getRazorpayOrderId());
        assertEquals(razorpayPaymentId, response.getRazorpayPaymentId());

        ArgumentCaptor<Order> savedOrderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrderCaptor.capture());
        Order savedOrder = savedOrderCaptor.getValue();

        assertEquals("SUCCESS", savedOrder.getStatus());
        assertEquals(razorpayPaymentId, savedOrder.getRazorpayPaymentId());
        assertEquals(signature, savedOrder.getRazorpaySignature());
        assertNotNull(savedOrder.getPaidAt());
    }

    @Test
    void verifyPayment_invalidSignature_marksOrderFailed() {
        String razorpayOrderId = "order_test_invalid";
        String razorpayPaymentId = "pay_test_invalid";
        String secret = "test_secret_key";

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("500.00"))
                .platformFee(new BigDecimal("100.00"))
                .razorpayOrderId(razorpayOrderId)
                .status("PENDING")
                .build();

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId(razorpayOrderId);
        request.setRazorpayPaymentId(razorpayPaymentId);
        request.setRazorpaySignature("invalid_signature");

        when(orderRepository.findByRazorpayOrderId(razorpayOrderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayConfig.getKeySecret()).thenReturn(secret);

        PaymentVerificationResponse response = paymentService.verifyPayment(request);

        assertFalse(response.isVerified());
        assertEquals("FAILED", response.getStatus());

        ArgumentCaptor<Order> savedOrderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrderCaptor.capture());
        Order savedOrder = savedOrderCaptor.getValue();

        assertEquals("FAILED", savedOrder.getStatus());
        assertEquals(razorpayPaymentId, savedOrder.getRazorpayPaymentId());
        assertEquals("invalid_signature", savedOrder.getRazorpaySignature());
        assertNull(savedOrder.getPaidAt());
    }

    @Test
    void verifyPayment_orderNotFound_throwsException() {
        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_missing");
        request.setRazorpayPaymentId("pay_missing");
        request.setRazorpaySignature("signature");

        when(orderRepository.findByRazorpayOrderId("order_missing")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.verifyPayment(request));
        assertTrue(ex.getMessage().contains("Order not found"));

        verify(orderRepository, never()).save(any(Order.class));
    }
}
