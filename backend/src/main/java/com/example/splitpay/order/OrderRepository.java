package com.example.splitpay.order;

import com.example.splitpay.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    List<Order> findByStatus(String status);
    List<Order> findByVendorId(UUID vendorId);
}
