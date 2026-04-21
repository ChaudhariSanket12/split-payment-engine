const API_BASE = 'http://localhost:8080/api';

// ─── On Load ─────────────────────────────────────────
window.addEventListener('DOMContentLoaded', () => {
  loadVendors();
  setupAmountPreview();
});

// ─── Load Vendors ────────────────────────────────────
async function loadVendors() {
  try {
    const res = await fetch(`${API_BASE}/vendors`);
    const vendors = await res.json();

    const select = document.getElementById('vendorSelect');
    select.innerHTML = vendors.length === 0
      ? '<option value="">No vendors found — add one below</option>'
      : '<option value="">Select a vendor...</option>';

    vendors.forEach(v => {
      const opt = document.createElement('option');
      opt.value = v.id;
      opt.textContent = `${v.name} (${v.email})`;
      select.appendChild(opt);
    });

    const tbody = document.getElementById('vendorsBody');

    if (vendors.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4">No vendors yet</td></tr>`;
      return;
    }

    tbody.innerHTML = vendors.map(v => `
      <tr>
        <td><strong>${escHtml(v.name)}</strong></td>
        <td>${escHtml(v.email)}</td>
        <td><code>${escHtml(v.razorpayAccountId || '—')}</code></td>
        <td>${formatDate(v.createdAt)}</td>
      </tr>
    `).join('');

  } catch (err) {
    console.error(err);
    showError("Failed to load vendors");
  }
}

// ─── Amount Preview ─────────────────────────────────
function setupAmountPreview() {
  const amountInput = document.getElementById('amount');

  amountInput.addEventListener('input', () => {
    const val = parseFloat(amountInput.value);
    const preview = document.getElementById('amountPreview');

    if (!val || val <= 0) {
      preview.style.display = 'none';
      return;
    }

    const platform = (val * 0.20).toFixed(2);
    const vendor = (val * 0.80).toFixed(2);

    document.getElementById('previewTotal').textContent = `₹${val}`;
    document.getElementById('previewVendor').textContent = `₹${vendor}`;
    document.getElementById('previewPlatform').textContent = `₹${platform}`;

    preview.style.display = 'flex';
  });
}

// ─── Create Order ───────────────────────────────────
async function createOrder() {
  const vendorId = document.getElementById('vendorSelect').value;
  const email = document.getElementById('customerEmail').value.trim();
  const amount = parseFloat(document.getElementById('amount').value);

  if (!vendorId) return showError("Select vendor");
  if (!email) return showError("Enter email");
  if (!amount || amount <= 0) return showError("Invalid amount");

  setPayBtnLoading(true);

  try {
    const res = await fetch(`${API_BASE}/orders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ vendorId, customerEmail: email, amount })
    });

    const data = await res.json();

    if (!res.ok) {
      showResponseError(data);
      return;
    }

    showResponseSuccess(data);
    openRazorpay(data, email);

  } catch (err) {
    showError("Network error");
  } finally {
    setPayBtnLoading(false);
  }
}

// ─── Razorpay Checkout ──────────────────────────────
function openRazorpay(orderData, email) {

  const options = {
    key: orderData.razorpayKeyId,
    amount: Math.round(orderData.amount * 100),
    currency: "INR",
    name: "SplitPay Engine",
    order_id: orderData.razorpayOrderId,

    prefill: { email },

    handler: async function (response) {
      console.log("Payment success:", response);

      try {
        const verifyRes = await fetch(`${API_BASE}/payments/verify`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            razorpayPaymentId: response.razorpay_payment_id,
            razorpayOrderId: response.razorpay_order_id,
            razorpaySignature: response.razorpay_signature
          })
        });

        const verifyData = await verifyRes.json();

        if (verifyData.verified) {
          setStatusBadge("✅ PAYMENT VERIFIED", "success");
          alert("Payment successful & verified!");
        } else {
          setStatusBadge("❌ VERIFICATION FAILED", "error");
        }

      } catch (err) {
        console.error(err);
        setStatusBadge("❌ VERIFY ERROR", "error");
      }
    },

    modal: {
      ondismiss: function () {
        setStatusBadge("⚠ Payment Cancelled", "error");
      }
    }
  };

  const rzp = new Razorpay(options);
  rzp.open();
}

// ─── Add Vendor ─────────────────────────────────────
async function addVendor() {
  const name = document.getElementById('vendorName').value.trim();
  const email = document.getElementById('vendorEmail').value.trim();

  if (!name || !email) return alert("Fill all fields");

  try {
    const res = await fetch(`${API_BASE}/vendors`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name, email })
    });

    const data = await res.json();

    if (!res.ok) return alert("Failed");

    alert("Vendor added");
    loadVendors();

  } catch (err) {
    alert("Network error");
  }
}

// ─── UI Helpers ─────────────────────────────────────
function showResponseSuccess(data) {
  document.getElementById('responseBox').textContent =
    JSON.stringify(data, null, 2);
  setStatusBadge("ORDER CREATED", "success");
}

function showResponseError(data) {
  document.getElementById('responseBox').textContent =
    JSON.stringify(data, null, 2);
  setStatusBadge("ERROR", "error");
}

function showError(msg) {
  setStatusBadge(msg, "error");
}

function setStatusBadge(text, type) {
  const badge = document.getElementById('statusBadge');
  badge.textContent = text;
  badge.className = `status-badge ${type}`;
  badge.style.display = "block";
}

function setPayBtnLoading(loading) {
  const btn = document.getElementById('payBtn');
  btn.disabled = loading;
}

// ─── Utils ─────────────────────────────────────────
function escHtml(str) {
  return str?.replace(/</g, "&lt;") || "";
}

function formatDate(dateStr) {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString();
}