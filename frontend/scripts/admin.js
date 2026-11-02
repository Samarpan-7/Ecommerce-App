/**
 * DEATH MART - Admin Dashboard Controller
 * Full management panel for products, orders, stats, and messages
 */

document.addEventListener('DOMContentLoaded', () => {
    checkAdminAccess();
});

function checkAdminAccess() {
    const user = Api.getCurrentUser();
    const token = Api.getToken();
    if (!token || !user || !user.roles || !user.roles.includes('ROLE_ADMIN')) {
        showToast('Access denied: Admin credentials required', 'error');
        setTimeout(() => window.location.href = './Login.html', 1000);
        return;
    }

    initAdmin();
}

async function initAdmin() {
    await loadAdminCategories();
    await loadDashboardStats();
    await loadAdminProducts();
    await loadAdminOrders();
    await loadAdminMessages();
    setupProductModal();
}

async function loadAdminCategories() {
    const categorySelect = document.getElementById('product-category');
    if (!categorySelect) return;

    try {
        const response = await Api.get('/categories');
        if (!response || !Array.isArray(response.data)) {
            throw new Error('The category service returned an invalid response.');
        }

        categorySelect.innerHTML = '<option value="">Select a category</option>' +
            response.data.map(category =>
                `<option value="${Number(category.id)}">${escapeHtml(category.name)}</option>`
            ).join('');
    } catch (error) {
        categorySelect.innerHTML = '<option value="">Categories unavailable</option>';
        showToast(`Unable to load product categories: ${error.message}`, 'error');
    }
}

async function loadDashboardStats() {
    try {
        const response = await Api.get('/admin/dashboard');
        if (response && response.data) {
            const stats = response.data;
            document.getElementById('stat-revenue').textContent = `₹${stats.totalRevenue.toLocaleString('en-IN')}`;
            document.getElementById('stat-orders').textContent = stats.totalOrders;
            document.getElementById('stat-products').textContent = stats.totalProducts;
            document.getElementById('stat-users').textContent = stats.totalUsers;
            document.getElementById('stat-pending-orders').textContent = stats.pendingOrders;
            document.getElementById('stat-low-stock').textContent = stats.lowStockProducts;
        }
    } catch (e) {
        console.error('Failed to load stats', e);
    }
}

async function loadAdminProducts() {
    const tableBody = document.getElementById('admin-products-table-body');
    if (!tableBody) return;

    try {
        const response = await Api.get('/products', { page: 0, size: 100, sortBy: 'id', sortDir: 'asc' });
        if (response && response.data && Array.isArray(response.data.content)) {
            const products = [...response.data.content];
            for (let page = 1; page < response.data.totalPages; page += 1) {
                const nextPage = await Api.get('/products', { page, size: 100, sortBy: 'id', sortDir: 'asc' });
                if (!nextPage || !nextPage.data || !Array.isArray(nextPage.data.content)) {
                    throw new Error('The product service returned an invalid response.');
                }
                products.push(...nextPage.data.content);
            }

            tableBody.innerHTML = products.map(p => `
                <tr>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">${p.id}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color); display: flex; align-items: center; gap: 10px;">
                        <img src="${escapeHtml(safeImageUrl(p.imageUrl))}" alt="" style="width: 36px; height: 36px; border-radius: 4px; object-fit: cover;">
                        <strong>${escapeHtml(p.name)}</strong>
                    </td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">${escapeHtml(p.categoryName || '-')}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">₹${p.price.toLocaleString('en-IN')}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">${p.discountPrice ? `₹${p.discountPrice.toLocaleString('en-IN')}` : '-'}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color); font-weight: 600; color: ${p.stockQuantity < 5 ? 'var(--primary-red)' : 'var(--badge-green)'};">
                        ${p.stockQuantity}
                    </td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">
                        <button class="btn btn-secondary btn-sm" onclick="editProduct(${p.id})">Edit</button>
                        <button class="btn btn-danger btn-sm" onclick="deleteProduct(${p.id})">Delete</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch (e) {
        console.error('Failed to load products for admin', e);
    }
}

async function loadAdminOrders() {
    const tableBody = document.getElementById('admin-orders-table-body');
    if (!tableBody) return;

    try {
        const response = await Api.get('/admin/orders', { size: 30 });
        if (response && response.data && response.data.content) {
            tableBody.innerHTML = response.data.content.map(o => `
                <tr>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color); font-family: monospace;">${o.orderNumber}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">${o.userEmail}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">₹${o.totalAmount ? o.totalAmount.toLocaleString('en-IN') : '0'}</td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color);">
                        <select onchange="updateOrderStatus(${o.id}, this.value)" style="padding: 4px 8px; font-size: 12px;">
                            <option value="PENDING" ${o.status === 'PENDING' ? 'selected' : ''}>PENDING</option>
                            <option value="PROCESSING" ${o.status === 'PROCESSING' ? 'selected' : ''}>PROCESSING</option>
                            <option value="SHIPPED" ${o.status === 'SHIPPED' ? 'selected' : ''}>SHIPPED</option>
                            <option value="DELIVERED" ${o.status === 'DELIVERED' ? 'selected' : ''}>DELIVERED</option>
                            <option value="CANCELLED" ${o.status === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                        </select>
                    </td>
                    <td style="padding: 12px; border-bottom: 1px solid var(--border-color); font-size: 12px; color: var(--text-dim);">
                        ${o.orderDate ? new Date(o.orderDate).toLocaleDateString() : '-'}
                    </td>
                </tr>
            `).join('');
        }
    } catch (e) {
        console.error('Failed to load orders for admin', e);
    }
}

async function updateOrderStatus(orderId, newStatus) {
    try {
        await Api.put(`/admin/orders/${orderId}/status`, { status: newStatus });
        showToast(`Order status updated to ${newStatus}`, 'success');
        loadDashboardStats();
    } catch (e) {
        showToast(e.message || 'Failed to update order status', 'error');
        loadAdminOrders();
    }
}

async function loadAdminMessages() {
    const container = document.getElementById('admin-messages-container');
    if (!container) return;

    try {
        const response = await Api.get('/admin/messages');
        if (response && response.data) {
            if (response.data.length === 0) {
                container.innerHTML = `<p style="color: var(--text-muted); font-size: 14px;">No customer queries yet.</p>`;
                return;
            }

            container.innerHTML = response.data.map(m => `
                <div style="background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 14px; margin-bottom: 12px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <strong>${m.name} (${m.email})</strong>
                        <span style="font-size: 12px; color: var(--text-dim);">${new Date(m.createdAt).toLocaleDateString()}</span>
                    </div>
                    <div style="font-weight: 600; color: var(--primary-red); font-size: 13px; margin-bottom: 4px;">${m.subject}</div>
                    <p style="font-size: 13px; color: var(--text-muted);">${m.message}</p>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('Failed to load messages', e);
    }
}

function setupProductModal() {
    const modal = document.getElementById('product-modal');
    const openBtn = document.getElementById('add-product-btn');
    const closeBtn = document.getElementById('close-product-modal');
    const form = document.getElementById('product-form');
    const imageInput = document.getElementById('product-image');
    const imagePreview = document.getElementById('product-image-preview');
    let previewObjectUrl = null;

    if (!modal) return;

    const clearImagePreview = () => {
        if (previewObjectUrl) {
            URL.revokeObjectURL(previewObjectUrl);
            previewObjectUrl = null;
        }
        imagePreview.removeAttribute('src');
        imagePreview.hidden = true;
        imagePreview.style.display = 'none';
    };

    const showImagePreview = (imageUrl) => {
        clearImagePreview();
        if (!imageUrl) return;
        imagePreview.src = imageUrl;
        imagePreview.hidden = false;
        imagePreview.style.display = 'block';
    };

    if (imageInput) {
        imageInput.addEventListener('change', () => {
            const image = imageInput.files[0];
            if (!image) {
                showImagePreview(document.getElementById('product-existing-image-url').value);
                return;
            }

            const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
            const allowedExtensions = /\.(jpe?g|png|webp)$/i;
            if ((!allowedTypes.includes(image.type) && !allowedExtensions.test(image.name)) ||
                image.size > 5 * 1024 * 1024) {
                imageInput.value = '';
                clearImagePreview();
                showToast('Choose a JPG, JPEG, PNG, or WEBP image up to 5 MB', 'error');
                return;
            }

            clearImagePreview();
            previewObjectUrl = URL.createObjectURL(image);
            imagePreview.src = previewObjectUrl;
            imagePreview.hidden = false;
            imagePreview.style.display = 'block';
        });
    }

    if (openBtn) {
        openBtn.addEventListener('click', () => {
            document.getElementById('modal-title').textContent = 'Add Cursed Product';
            form.reset();
            document.getElementById('product-id').value = '';
            document.getElementById('product-existing-image-url').value = '';
            clearImagePreview();
            modal.classList.add('active');
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => modal.classList.remove('active'));
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('product-id').value;
            const categoryId = Number(document.getElementById('product-category').value);
            if (!Number.isInteger(categoryId) || categoryId < 1) {
                showToast('Choose a valid product category', 'error');
                return;
            }
            const payload = {
                name: document.getElementById('product-name').value.trim(),
                description: document.getElementById('product-desc').value.trim(),
                price: parseFloat(document.getElementById('product-price').value),
                discountPrice: document.getElementById('product-discount').value ? parseFloat(document.getElementById('product-discount').value) : null,
                stockQuantity: parseInt(document.getElementById('product-stock').value, 10),
                imageUrl: document.getElementById('product-existing-image-url').value,
                categoryId
            };

            try {
                const image = imageInput.files[0];
                if (image) {
                    if (image.size > 5 * 1024 * 1024) {
                        throw new Error('Product images must be 5 MB or smaller');
                    }
                    const formData = new FormData();
                    formData.append('image', image);
                    const uploadResponse = await Api.upload('/products/images', formData);
                    if (!uploadResponse || !uploadResponse.data || !uploadResponse.data.imageUrl) {
                        throw new Error('The image service returned an invalid response.');
                    }
                    payload.imageUrl = uploadResponse.data.imageUrl;
                }

                if (id) {
                    await Api.put(`/products/${id}`, payload);
                    showToast('Product updated successfully', 'success');
                } else {
                    await Api.post('/products', payload);
                    showToast('Product added to Death Mart catalog', 'success');
                }
                modal.classList.remove('active');
                clearImagePreview();
                await Promise.all([loadAdminProducts(), loadDashboardStats()]);
            } catch (err) {
                showToast(err.message || 'Operation failed', 'error');
            }
        });
    }
}

async function editProduct(id) {
    try {
        const response = await Api.get(`/products/${id}`);
        if (response && response.data) {
            const p = response.data;
            document.getElementById('modal-title').textContent = 'Edit Product';
            document.getElementById('product-id').value = p.id;
            document.getElementById('product-name').value = p.name;
            document.getElementById('product-desc').value = p.description || '';
            document.getElementById('product-price').value = p.price;
            document.getElementById('product-discount').value = p.discountPrice || '';
            document.getElementById('product-stock').value = p.stockQuantity;
            document.getElementById('product-image').value = '';
            document.getElementById('product-existing-image-url').value = p.imageUrl || '';
            document.getElementById('product-category').value = p.categoryId || '';
            const imagePreview = document.getElementById('product-image-preview');
            if (p.imageUrl) {
                imagePreview.src = safeImageUrl(p.imageUrl);
                imagePreview.hidden = false;
                imagePreview.style.display = 'block';
            } else {
                imagePreview.removeAttribute('src');
                imagePreview.hidden = true;
                imagePreview.style.display = 'none';
            }

            document.getElementById('product-modal').classList.add('active');
        }
    } catch (e) {
        showToast('Failed to load product details', 'error');
    }
}

async function deleteProduct(id) {
    if (!confirm('Are you sure you want to delete this product?')) return;
    try {
        await Api.delete(`/products/${id}`);
        showToast('Product deleted', 'success');
        loadAdminProducts();
        loadDashboardStats();
    } catch (e) {
        showToast(e.message || 'Failed to delete product', 'error');
    }
}

window.editProduct = editProduct;
window.deleteProduct = deleteProduct;
window.updateOrderStatus = updateOrderStatus;
