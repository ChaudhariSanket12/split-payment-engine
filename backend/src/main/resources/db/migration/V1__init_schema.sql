-- Split Payment Engine - Initial Schema
-- Run this in your Supabase SQL Editor before starting the application

CREATE TABLE IF NOT EXISTS vendor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    razorpay_account_id VARCHAR(50) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID REFERENCES vendor(id),
    customer_email VARCHAR(100),
    amount DECIMAL(10, 2) NOT NULL,
    platform_fee DECIMAL(10, 2) NOT NULL,
    razorpay_order_id VARCHAR(50) UNIQUE,
    razorpay_payment_id VARCHAR(50) UNIQUE,
    razorpay_signature VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP
);

-- Optional: seed a test vendor
-- INSERT INTO vendor (name, email, razorpay_account_id)
-- VALUES ('Demo Vendor', 'demo@vendor.com', 'acc_YOUR_TEST_ACCOUNT_ID');
