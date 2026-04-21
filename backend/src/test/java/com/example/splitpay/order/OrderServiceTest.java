package com.example.splitpay.order;

import com.example.splitpay.config.RazorpayConfig;
import com.example.splitpay.order.dto.CreateOrderRequest;
import com.example.splitpay.order.dto.OrderResponse;
import com.example.splitpay.order.model.Order;
import com.example.splitpay.vendor.VendorRepository;
import com.example.splitpay.vendor.model.Vendor;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private RazorpayConfig razorpayConfig;

    @InjectMocks
    private OrderService orderService;

    private Vendor mockVendor;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        mockVendor = Vendor.builder()
                .id(vendorId)
                .name("Test Vendor")
                .email("vendor@test.com")
                .razorpayAccountId("acc_test_123456789")
                .build();
    }

    @Test
    void createOrder_success_splitCalculatedCorrectly() throws RazorpayException {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setVendorId(vendorId);
        request.setCustomerEmail("customer@test.com");
        request.setAmount(new BigDecimal("1000.00"));

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(mockVendor));

        // Mock Razorpay order response
        JSONObject rzpOrderJson = new JSONObject();
        rzpOrderJson.put("id", "order_test_abc123");
        rzpOrderJson.put("amount", 100000);
        rzpOrderJson.put("currency", "INR");
        rzpOrderJson.put("status", "created");

        com.razorpay.Order mockRzpOrder = new com.razorpay.Order(rzpOrderJson);

        // Razorpay SDK exposes `orders` as a public field, so assign the mock directly.
        var mockOrdersClient = mock(com.razorpay.OrderClient.class);
        razorpayClient.orders = mockOrdersClient;
        when(mockOrdersClient.create(any(JSONObject.class))).thenReturn(mockRzpOrder);

        // Mock repository save
        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .vendor(mockVendor)
                .customerEmail("customer@test.com")
                .amount(new BigDecimal("1000.00"))
                .platformFee(new BigDecimal("200.00"))
                .razorpayOrderId("order_test_abc123")
                .status("PENDING")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(razorpayConfig.getKeyId()).thenReturn("rzp_test_key");

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals("order_test_abc123", response.getRazorpayOrderId());
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals(new BigDecimal("200.00"), response.getPlatformFee()); // 20% of 1000
        assertEquals(new BigDecimal("800.00"), response.getVendorAmount()); // 80% of 1000
        assertEquals("PENDING", response.getStatus());
        assertEquals("customer@test.com", response.getCustomerEmail());

        ArgumentCaptor<JSONObject> orderRequestCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(vendorRepository, times(1)).findById(vendorId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(mockOrdersClient, times(1)).create(orderRequestCaptor.capture());

        JSONObject capturedOrderRequest = orderRequestCaptor.getValue();
        assertEquals(100000L, capturedOrderRequest.getLong("amount"));
        assertEquals("INR", capturedOrderRequest.getString("currency"));
        assertTrue(capturedOrderRequest.has("receipt"));
        assertFalse(capturedOrderRequest.has("transfers"));
        assertFalse(capturedOrderRequest.has("account_id"));
        assertFalse(capturedOrderRequest.has("payment_capture"));
    }

    @Test
    void createOrder_vendorNotFound_throwsException() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setVendorId(UUID.randomUUID());
        request.setAmount(new BigDecimal("500.00"));
        request.setCustomerEmail("c@test.com");

        when(vendorRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.createOrder(request));
        assertTrue(ex.getMessage().contains("Vendor not found"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_vendorWithoutRazorpayAccount_stillCreatesOrder() throws RazorpayException {
        // Arrange
        Vendor vendorNoAccount = Vendor.builder()
                .id(vendorId)
                .name("No Account Vendor")
                .email("noaccount@test.com")
                .razorpayAccountId(null)
                .build();

        CreateOrderRequest request = new CreateOrderRequest();
        request.setVendorId(vendorId);
        request.setAmount(new BigDecimal("500.00"));
        request.setCustomerEmail("c@test.com");

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendorNoAccount));

        JSONObject rzpOrderJson = new JSONObject();
        rzpOrderJson.put("id", "order_test_no_account");
        com.razorpay.Order mockRzpOrder = new com.razorpay.Order(rzpOrderJson);

        var mockOrdersClient = mock(com.razorpay.OrderClient.class);
        razorpayClient.orders = mockOrdersClient;
        when(mockOrdersClient.create(any(JSONObject.class))).thenReturn(mockRzpOrder);

        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .vendor(vendorNoAccount)
                .customerEmail("c@test.com")
                .amount(new BigDecimal("500.00"))
                .platformFee(new BigDecimal("100.00"))
                .razorpayOrderId("order_test_no_account")
                .status("PENDING")
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(razorpayConfig.getKeyId()).thenReturn("rzp_test_key");

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals("order_test_no_account", response.getRazorpayOrderId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}
