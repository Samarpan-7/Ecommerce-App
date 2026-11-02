/**
 * DEATH MART - Checkout Controller
 * Manages Shipping Address selection, Payment Method, and Order Submission
 */

let selectedAddressId = null;

document.addEventListener('DOMContentLoaded', () => {
    initCheckout();
});

async function initCheckout() {
    const token = Api.getToken();
    if (!token) {
        showToast('Please sign in to proceed with checkout', 'info');
        setTimeout(() => window.location.href = './Login.html', 1000);
        return;
    }

    await loadCheckoutCart();
    await loadUserAddresses();
    setupCheckoutForm();
}

async function loadCheckoutCart() {
    const summaryList = document.getElementById('checkout-items-list');
    const totalAmountEl = document.getElementById('checkout-total-amount');
    if (!summaryList) return;

    try {
        const response = await Api.get('/cart');
        if (response && response.data) {
            const cart = response.data;
            if (!cart.items || cart.items.length === 0) {
                showToast('Your cart is empty. Redirecting to catalog...', 'info');
                setTimeout(() => window.location.href = './Products.html', 1000);
                return;
            }

            summaryList.innerHTML = cart.items.map(item => `
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 14px;">
                    <div>
                        <strong style="color: var(--text-main);">${item.productName}</strong>
                        <div style="color: var(--text-muted); font-size: 12px;">Qty: ${item.quantity} × ₹${item.unitPrice.toLocaleString('en-IN')}</div>
                    </div>
                    <span style="font-weight: 600; color: var(--primary-red);">₹${item.subtotal.toLocaleString('en-IN')}</span>
                </div>
            `).join('');

            const subtotal = cart.totalAmount || 0;
            const shipping = subtotal > 999 ? 0 : 99;
            const grandTotal = subtotal + shipping;

            if (totalAmountEl) {
                totalAmountEl.textContent = `₹${grandTotal.toLocaleString('en-IN')}`;
            }

            const subtotalEl = document.getElementById('checkout-subtotal');
            if (subtotalEl) subtotalEl.textContent = `₹${subtotal.toLocaleString('en-IN')}`;

            const shippingEl = document.getElementById('checkout-shipping');
            if (shippingEl) shippingEl.textContent = shipping === 0 ? 'FREE' : `₹${shipping}`;
        }
    } catch (e) {
        showToast('Failed to load order summary', 'error');
    }
}

async function loadUserAddresses() {
    const addressContainer = document.getElementById('saved-addresses-container');
    if (!addressContainer) return;

    try {
        const response = await Api.get('/addresses');
        if (response && response.data && response.data.length > 0) {
            addressContainer.innerHTML = response.data.map(addr => `
                <label style="display: flex; gap: 12px; padding: 14px; background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: var(--radius-sm); margin-bottom: 10px; cursor: pointer;">
                    <input type="radio" name="addressOption" value="${addr.id}" ${addr.isDefault ? 'checked' : ''} onchange="selectAddress(${addr.id})">
                    <div>
                        <strong>${addr.street}</strong>
                        <div style="color: var(--text-muted); font-size: 13px;">${addr.city}, ${addr.state} - ${addr.zipCode}, ${addr.country}</div>
                        ${addr.isDefault ? '<span class="badge badge-red" style="margin-top: 4px;">Default</span>' : ''}
                    </div>
                </label>
            `).join('');

            // Select default
            const defaultAddr = response.data.find(a => a.isDefault) || response.data[0];
            selectedAddressId = defaultAddr.id;
        } else {
            addressContainer.innerHTML = `<p style="font-size: 13px; color: var(--text-muted); margin-bottom: 12px;">No saved address found. Please enter your shipping address below.</p>`;
            document.getElementById('new-address-toggle').checked = true;
            toggleNewAddressForm(true);
        }
    } catch (e) {
        console.error('Failed to load addresses', e);
    }
}

function selectAddress(id) {
    selectedAddressId = id;
    document.getElementById('new-address-toggle').checked = false;
    toggleNewAddressForm(false);
}

function toggleNewAddressForm(show) {
    const form = document.getElementById('new-address-fields');
    if (form) {
        form.style.display = show ? 'block' : 'none';
        if (show) selectedAddressId = null;
    }
}

function setupCheckoutForm() {
    const placeOrderBtn = document.getElementById('place-order-btn');
    if (!placeOrderBtn) return;

    placeOrderBtn.addEventListener('click', async () => {
        placeOrderBtn.disabled = true;
        placeOrderBtn.textContent = 'Processing Judgment...';

        const paymentMethodEl = document.querySelector('input[name="paymentMethod"]:checked');
        const paymentMethod = paymentMethodEl ? paymentMethodEl.value : 'UPI';

        const orderRequest = {
            paymentMethod: paymentMethod
        };

        const isNewAddress = document.getElementById('new-address-toggle')?.checked;
        if (isNewAddress || !selectedAddressId) {
            const street = document.getElementById('street')?.value.trim();
            const city = document.getElementById('city')?.value.trim();
            const state = document.getElementById('state')?.value.trim();
            const zipCode = document.getElementById('zipCode')?.value.trim();
            const country = document.getElementById('country')?.value.trim() || 'India';

            if (!street || !city || !state || !zipCode) {
                showToast('Please fill in all address fields', 'error');
                placeOrderBtn.disabled = false;
                placeOrderBtn.textContent = 'Place Order & Complete Pact';
                return;
            }

            orderRequest.shippingAddress = { street, city, state, zipCode, country, isDefault: true };
        } else {
            orderRequest.shippingAddressId = selectedAddressId;
        }

        try {
            const response = await Api.post('/orders', orderRequest);
            if (response && response.data) {
                showToast('Order confirmed! Order #' + response.data.orderNumber, 'success');
                updateCartBadge();
                setTimeout(() => {
                    window.location.href = './Orders.html?placed=' + response.data.orderNumber;
                }, 1200);
            }
        } catch (error) {
            showToast(error.message || 'Failed to place order', 'error');
            placeOrderBtn.disabled = false;
            placeOrderBtn.textContent = 'Place Order & Complete Pact';
        }
    });
}

window.selectAddress = selectAddress;
window.toggleNewAddressForm = toggleNewAddressForm;
