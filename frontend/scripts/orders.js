/**
 * DEATH MART - Orders & Tracking Controller
 * Fetches user order history and provides order tracking
 */

document.addEventListener('DOMContentLoaded', () => {
    initOrders();
});

async function initOrders() {
    const token = Api.getToken();
    if (!token) {
        showToast('Please sign in to view your orders', 'info');
        setTimeout(() => window.location.href = './Login.html', 1000);
        return;
    }

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('placed')) {
        showToast('Pact sealed! Order #' + urlParams.get('placed'), 'success');
    }

    await loadOrders();
    setupTrackingSearch();
}

async function loadOrders() {
    const container = document.getElementById('orders-container');
    if (!container) return;

    container.innerHTML = `
        <div style="text-align: center; padding: 40px; color: var(--text-muted);">
            <div>⏳</div>
            <p>Retrieving your sealed transactions...</p>
        </div>
    `;

    try {
        const response = await Api.get('/orders');
        if (response && response.data && response.data.content) {
            renderOrdersList(response.data.content);
        }
    } catch (e) {
        container.innerHTML = `
            <div style="text-align: center; padding: 40px; color: var(--text-muted);">
                <p>Failed to load orders: ${e.message}</p>
            </div>
        `;
    }
}

function renderOrdersList(orders) {
    const container = document.getElementById('orders-container');
    if (!orders || orders.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 60px 20px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md);">
                <div style="font-size: 40px; margin-bottom: 12px;">📜</div>
                <h3>No orders placed yet</h3>
                <p style="color: var(--text-muted); font-size: 14px; margin: 8px 0 20px;">You haven't bound any artifacts to your name.</p>
                <a href="./Products.html" class="btn btn-primary">Browse Catalog</a>
            </div>
        `;
        return;
    }

    container.innerHTML = orders.map(order => {
        let statusBadgeClass = 'badge-yellow';
        if (order.status === 'DELIVERED') statusBadgeClass = 'badge-green';
        if (order.status === 'CANCELLED') statusBadgeClass = 'badge-red';
        if (order.status === 'SHIPPED') statusBadgeClass = 'badge-blue';

        const orderDateStr = order.orderDate ? new Date(order.orderDate).toLocaleDateString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        }) : '';

        return `
            <div class="order-card" style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 24px; margin-bottom: 20px;">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; padding-bottom: 14px; border-bottom: 1px solid var(--border-color); flex-wrap: wrap; gap: 12px;">
                    <div>
                        <div style="font-size: 13px; color: var(--text-muted);">Order Number</div>
                        <strong style="font-size: 16px; color: var(--text-main); font-family: monospace;">${order.orderNumber}</strong>
                        <div style="font-size: 12px; color: var(--text-dim); margin-top: 2px;">Placed on: ${orderDateStr}</div>
                    </div>

                    <div style="text-align: right;">
                        <span class="badge ${statusBadgeClass}">${order.status}</span>
                        <div style="font-size: 18px; font-weight: 700; color: var(--primary-red); margin-top: 6px;">
                            ₹${order.totalAmount ? order.totalAmount.toLocaleString('en-IN') : '0'}
                        </div>
                    </div>
                </div>

                <div class="order-items-list" style="margin-bottom: 16px;">
                    ${order.items.map(item => `
                        <div style="display: flex; gap: 14px; align-items: center; margin-bottom: 10px;">
                            <img src="${item.productImageUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600'}" alt="" style="width: 50px; height: 50px; border-radius: 4px; object-fit: cover;">
                            <div style="flex: 1;">
                                <div style="font-size: 14px; font-weight: 600; color: var(--text-main);">${item.productName}</div>
                                <div style="font-size: 12px; color: var(--text-muted);">Qty: ${item.quantity} × ₹${item.unitPrice ? item.unitPrice.toLocaleString('en-IN') : '0'}</div>
                            </div>
                            <div style="font-size: 14px; font-weight: 600; color: var(--text-main);">
                                ₹${item.subtotal ? item.subtotal.toLocaleString('en-IN') : '0'}
                            </div>
                        </div>
                    `).join('')}
                </div>

                <div style="display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid var(--border-color); font-size: 12px; color: var(--text-muted);">
                    <div>
                        📍 Shipping to: ${order.shippingAddress ? `${order.shippingAddress.street}, ${order.shippingAddress.city}` : 'Default Address'}
                    </div>
                    <div>
                        💳 Payment: ${order.payment ? order.payment.paymentMethod : 'Online'} (${order.payment ? order.payment.paymentStatus : 'PAID'})
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function setupTrackingSearch() {
    const trackForm = document.getElementById('order-track-form');
    if (!trackForm) return;

    trackForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const orderNumber = document.getElementById('track-order-number').value.trim();
        if (!orderNumber) return;

        try {
            const response = await Api.get(`/orders/track/${encodeURIComponent(orderNumber)}`);
            if (response && response.data) {
                renderOrdersList([response.data]);
                showToast('Found order ' + orderNumber, 'success');
            }
        } catch (error) {
            showToast(error.message || 'Order not found with number ' + orderNumber, 'error');
        }
    });
}
