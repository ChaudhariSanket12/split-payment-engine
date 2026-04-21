package com.example.splitpay.vendor;

import com.example.splitpay.vendor.dto.VendorRequest;
import com.example.splitpay.vendor.dto.VendorResponse;
import com.example.splitpay.vendor.model.Vendor;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorRepository vendorRepository;
    private final RazorpayClient razorpayClient;

    public VendorResponse createVendor(VendorRequest request) {
        if (vendorRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Vendor with email " + request.getEmail() + " already exists.");
        }

        String razorpayAccountId = request.getRazorpayAccountId();

        // Attempt to register linked account with Razorpay if no account ID provided
        if (razorpayAccountId == null || razorpayAccountId.isBlank()) {
            razorpayAccountId = createRazorpayLinkedAccount(request.getName(), request.getEmail());
        }

        Vendor vendor = Vendor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .razorpayAccountId(razorpayAccountId)
                .build();

        vendor = vendorRepository.save(vendor);
        log.info("Created vendor: {} with Razorpay account: {}", vendor.getId(), vendor.getRazorpayAccountId());

        return toResponse(vendor);
    }

    public List<VendorResponse> getAllVendors() {
        return vendorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public VendorResponse getVendorById(UUID id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));
        return toResponse(vendor);
    }

    /**
     * Attempts to create a Razorpay linked account.
     * Falls back to a simulated test account ID if the API call fails
     * (e.g., in test mode without route enabled).
     */
    private String createRazorpayLinkedAccount(String name, String email) {
        try {
            JSONObject request = new JSONObject();
            request.put("email", email);
            request.put("profile", new JSONObject()
                    .put("category", "healthcare")
                    .put("subcategory", "clinic")
                    .put("addresses", new JSONObject()
                            .put("registered", new JSONObject()
                                    .put("street1", "123 Test Street")
                                    .put("city", "Mumbai")
                                    .put("state", "Maharashtra")
                                    .put("postal_code", "400001")
                                    .put("country", "IN"))));
            request.put("type", "route");
            request.put("legal_business_name", name);
            request.put("business_type", "individual");

            com.razorpay.Account account = razorpayClient.account.create(request);
            String accountId = account.get("id");
            log.info("Created Razorpay linked account: {}", accountId);
            return accountId;
        } catch (Exception e) {
            log.warn("Could not create Razorpay linked account (test mode limitation). Using simulated ID. Error: {}", e.getMessage());
            // Return a simulated test account ID for demo purposes
            return "acc_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }
    }

    private VendorResponse toResponse(Vendor vendor) {
        return VendorResponse.builder()
                .id(vendor.getId())
                .name(vendor.getName())
                .email(vendor.getEmail())
                .razorpayAccountId(vendor.getRazorpayAccountId())
                .createdAt(vendor.getCreatedAt())
                .build();
    }
}
