/**
 * DEATH MART - Shopping Cart Controller
 * Fetches user cart from backend, manages item quantities, removals, and total calculations
 */

document.addEventListener('DOMContentLoaded', () => {
    loadCart();
});

async function loadCart() {
    const token = Api.getToken();
    if (!token) {
        showEmptyCart('Please sign in to view your cursed shopping cart.');
        return;
    }

    const container = document.getElementById('cart-items-container');
    const summaryContainer = document.getElementById('cart-summary-container');
    if (!container) return;

    try {
        const response = await Api.get('/cart');
        if (response && response.data) {
            renderCart(response.data);
        }
    } catch (error) {
        showCartError(error.message);
    }
}

function renderCart(cart) {
    const itemsContainer = document.getElementById('cart-items-container');
    const summaryContainer = document.getElementById('cart-summary-container');

    if (!cart.items || cart.items.length === 0) {
        showEmptyCart('Your cursed cart is empty.');
        updateCartBadge();
        return;
    }

    // Render items list
    itemsContainer.innerHTML = cart.items.map(item => `
        <div class="cart-item-card" style="display: flex; gap: 18px; align-items: center; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 16px; margin-bottom: 14px;">
            <img src="${escapeHtml(safeImageUrl(item.productImageUrl))}" alt="${escapeHtml(item.productName)}" style="width: 80px; height: 80px; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
            
            <div style="flex: 1;">
                <h4 style="font-size: 16px; font-weight: 600; color: var(--text-main); margin-bottom: 4px;">${escapeHtml(item.productName)}</h4>
                <div style="font-size: 13px; color: var(--text-muted);">Unit Price: ₹${item.unitPrice.toLocaleString('en-IN')}</div>
            </div>

            <div style="display: flex; align-items: center; gap: 8px;">
                <button class="btn btn-secondary btn-sm" onclick="updateItemQuantity(${item.id}, ${item.quantity - 1})" style="width: 32px; height: 32px; padding: 0;">-</button>
                <span style="font-size: 15px; font-weight: 600; min-width: 24px; text-align: center;">${item.quantity}</span>
                <button class="btn btn-secondary btn-sm" onclick="updateItemQuantity(${item.id}, ${item.quantity + 1})" style="width: 32px; height: 32px; padding: 0;">+</button>
            </div>

            <div style="min-width: 100px; text-align: right;">
                <div style="font-size: 16px; font-weight: 700; color: var(--primary-red);">₹${item.subtotal.toLocaleString('en-IN')}</div>
                <button onclick="removeItem(${item.id})" style="background: none; border: none; color: var(--text-dim); font-size: 12px; cursor: pointer; margin-top: 4px; text-decoration: underline;">
                    Remove
                </button>
            </div>
        </div>
    `).join('');

    // Calculations
    const subtotal = cart.totalAmount || 0;
    if (summaryContainer) {
        summaryContainer.innerHTML = `
            <div style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 24px;">
                <h3 style="font-size: 18px; font-weight: 600; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid var(--border-color);">
                    Order Summary
                </h3>

                <div style="display: flex; justify-content: space-between; margin-bottom: 16px; color: var(--text-muted); font-size: 14px;">
                    <span>Subtotal (${cart.totalQuantity} items)</span>
                    <span>₹${subtotal.toLocaleString('en-IN')}</span>
                </div>

                <div style="display: flex; justify-content: space-between; padding-top: 16px; border-top: 1px solid var(--border-color); margin-bottom: 24px; font-size: 18px; font-weight: 700; color: var(--text-main);">
                    <span>Total</span>
                    <span style="color: var(--primary-red);">₹${subtotal.toLocaleString('en-IN')}</span>
                </div>

                <button class="btn btn-secondary btn-full btn-sm" onclick="clearCart()">
                    Clear Cart
                </button>
            </div>
        `;
    }

    updateCartBadge();
}

function showEmptyCart(message) {
    const itemsContainer = document.getElementById('cart-items-container');
    const summaryContainer = document.getElementById('cart-summary-container');

    if (itemsContainer) {
        itemsContainer.innerHTML = `
            <div style="text-align: center; padding: 60px 20px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md);">
                <div style="font-size: 48px; margin-bottom: 16px;">🛒</div>
                <h3 style="font-size: 20px; margin-bottom: 8px;">${escapeHtml(message)}</h3>
                <p style="color: var(--text-muted); font-size: 14px; margin-bottom: 24px;">Explore our cursed collection and summon items into your cart.</p>
                <a href="./Products.html" class="btn btn-primary">Browse Products</a>
            </div>
        `;
    }

    if (summaryContainer) {
        summaryContainer.innerHTML = '';
    }
}

function showCartError(message) {
    const itemsContainer = document.getElementById('cart-items-container');
    const summaryContainer = document.getElementById('cart-summary-container');
    if (itemsContainer) {
        itemsContainer.innerHTML = `
            <div style="text-align: center; padding: 60px 20px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md);">
                <h3 style="font-size: 20px; margin-bottom: 8px;">Unable to load your cart</h3>
                <p style="color: var(--text-muted);">${escapeHtml(message)}</p>
            </div>
        `;
    }
    if (summaryContainer) summaryContainer.innerHTML = '';
}

async function updateItemQuantity(itemId, newQuantity) {
    try {
        const response = await Api.put(`/cart/items/${itemId}`, { quantity: newQuantity });
        if (response && response.data) {
            renderCart(response.data);
            showToast('Cart updated', 'info');
        }
    } catch (error) {
        showToast(error.message || 'Failed to update quantity', 'error');
    }
}

async function removeItem(itemId) {
    try {
        const response = await Api.delete(`/cart/items/${itemId}`);
        if (response && response.data) {
            renderCart(response.data);
            showToast('Item removed from cart', 'info');
        }
    } catch (error) {
        showToast(error.message || 'Failed to remove item', 'error');
    }
}

async function clearCart() {
    if (!confirm('Are you sure you want to empty your cart?')) return;
    try {
        await Api.delete('/cart');
        showEmptyCart('Your cursed cart is empty.');
        showToast('Cart cleared', 'info');
        updateCartBadge();
    } catch (error) {
        showToast(error.message || 'Failed to clear cart', 'error');
    }
}

window.updateItemQuantity = updateItemQuantity;
window.removeItem = removeItem;
window.clearCart = clearCart;
