package com.example.splitpay.vendor;

import com.example.splitpay.vendor.model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByEmail(String email);
    boolean existsByEmail(String email);
}
