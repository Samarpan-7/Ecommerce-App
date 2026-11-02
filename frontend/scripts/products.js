/**
 * DEATH MART - Product Catalog Controller
 * Loads products from Spring Boot REST API, supports filtering, search, sorting, and add-to-cart
 */

let currentPage = 0;
let pageSize = 12;
let currentCategory = null;
let currentKeyword = '';
let currentSort = 'id';
let currentSortDir = 'desc';

document.addEventListener('DOMContentLoaded', () => {
    initCatalog();
});

async function initCatalog() {
    // Check URL parameters (e.g. ?category=1 or ?search=apple)
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('category')) {
        currentCategory = urlParams.get('category');
    }
    if (urlParams.has('search')) {
        currentKeyword = urlParams.get('search');
        const searchInput = document.getElementById('search-input');
        if (searchInput) searchInput.value = currentKeyword;
    }

    await loadCategories();
    await loadProducts();

    setupEventListeners();
}

function setupEventListeners() {
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                currentKeyword = e.target.value.trim();
                currentPage = 0;
                loadProducts();
            }, 400);
        });
    }

    const sortSelect = document.getElementById('sort-select');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            const val = e.target.value;
            if (val === 'price-asc') {
                currentSort = 'price';
                currentSortDir = 'asc';
            } else if (val === 'price-desc') {
                currentSort = 'price';
                currentSortDir = 'desc';
            } else if (val === 'name-asc') {
                currentSort = 'name';
                currentSortDir = 'asc';
            } else {
                currentSort = 'id';
                currentSortDir = 'desc';
            }
            currentPage = 0;
            loadProducts();
        });
    }
}

async function loadCategories() {
    const filterContainer = document.getElementById('category-filters');
    if (!filterContainer) return;

    try {
        const response = await Api.get('/categories');
        if (response && response.data) {
            let html = `<button class="category-chip ${!currentCategory ? 'active' : ''}" onclick="filterByCategory(null, this)">All Realms</button>`;
            response.data.forEach(cat => {
                html += `
                    <button class="category-chip ${currentCategory == cat.id ? 'active' : ''}" onclick="filterByCategory(${Number(cat.id)}, this)">
                        ${escapeHtml(cat.name)} (${Number(cat.productCount) || 0})
                    </button>
                `;
            });
            filterContainer.innerHTML = html;
        }
    } catch (e) {
        console.error('Failed to load categories:', e);
        showToast(`Unable to load categories: ${e.message}`, 'error');
    }
}

function filterByCategory(catId, button) {
    currentCategory = catId;
    currentPage = 0;
    document.querySelectorAll('.category-chip').forEach(chip => chip.classList.remove('active'));
    if (button) button.classList.add('active');
    loadProducts();
}

async function loadProducts() {
    const container = document.getElementById('products-grid');
    if (!container) return;

    container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: var(--text-muted);">
            <div style="font-size: 32px; margin-bottom: 12px;">⏳</div>
            <p>Summoning cursed inventory from Death Mart vault...</p>
        </div>
    `;

    try {
        const params = {
            page: currentPage,
            size: pageSize,
            sortBy: currentSort,
            sortDir: currentSortDir
        };
        if (currentCategory) params.categoryId = currentCategory;
        if (currentKeyword) params.keyword = currentKeyword;

        const response = await Api.get('/products', params);
        if (response && response.data) {
            renderProducts(response.data.content);
            renderPagination(response.data);
        } else {
            throw new Error('The product service returned an empty response.');
        }
    } catch (error) {
        container.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: #ff4d4f;">
                <p>⚠️ Unable to load products. Ensure the backend server is running on port 8080.</p>
                <p style="font-size: 13px; color: var(--text-muted); margin-top: 8px;">${escapeHtml(error.message)}</p>
            </div>
        `;
    }
}

function renderProducts(products) {
    const container = document.getElementById('products-grid');
    if (!products || products.length === 0) {
        container.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: var(--text-muted);">
                <div style="font-size: 40px; margin-bottom: 12px;">📓</div>
                <h3>No cursed artifacts found</h3>
                <p style="font-size: 14px; margin-top: 6px;">Try adjusting your search criteria or category filter.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = products.map(product => {
        const hasDiscount = product.discountPrice && product.discountPrice < product.price;
        const effectivePrice = hasDiscount ? product.discountPrice : product.price;
        const discountPercent = hasDiscount ? Math.round(((product.price - product.discountPrice) / product.price) * 100) : 0;

        return `
            <div class="product-card">
                <div class="product-card-img-wrap">
                            <img src="${escapeHtml(safeImageUrl(product.imageUrl))}" alt="${escapeHtml(product.name)}" loading="lazy">
                    ${hasDiscount ? `<span class="product-card-discount">-${discountPercent}% OFF</span>` : ''}
                </div>
                <div class="product-card-body">
                    <span class="product-category-tag">${escapeHtml(product.categoryName || 'Artifact')}</span>
                    <h3 class="product-card-title">${escapeHtml(product.name)}</h3>
                    <p class="product-card-desc">${escapeHtml(product.description || '')}</p>
                    
                    <div class="product-price-row">
                        <span class="product-price-current">₹${effectivePrice.toLocaleString('en-IN')}</span>
                        ${hasDiscount ? `<span class="product-price-original">₹${product.price.toLocaleString('en-IN')}</span>` : ''}
                        <span style="margin-left: auto; font-size: 11px; color: ${product.stockQuantity > 5 ? 'var(--badge-green)' : 'var(--badge-gold)'};">
                            ${product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : 'Out of stock'}
                        </span>
                    </div>

                    <div class="product-card-footer">
                        <button class="btn btn-secondary btn-sm" onclick="viewProductDetail(${product.id})">Details</button>
                        <button class="btn btn-primary btn-sm" onclick="addToCart(${product.id})" ${product.stockQuantity <= 0 ? 'disabled' : ''}>
                            ${product.stockQuantity <= 0 ? 'Out of Stock' : '+ Add to Cart'}
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function renderPagination(pageData) {
    const container = document.getElementById('pagination-container');
    if (!container) return;

    if (pageData.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = `
        <div style="display: flex; justify-content: center; align-items: center; gap: 8px; margin-top: 36px;">
            <button class="btn btn-secondary btn-sm" ${pageData.page === 0 ? 'disabled' : ''} onclick="changePage(${pageData.page - 1})">
                &larr; Prev
            </button>
            <span style="font-size: 13px; color: var(--text-muted); margin: 0 10px;">
                Page <strong>${pageData.page + 1}</strong> of <strong>${pageData.totalPages}</strong>
            </span>
            <button class="btn btn-secondary btn-sm" ${pageData.last ? 'disabled' : ''} onclick="changePage(${pageData.page + 1})">
                Next &rarr;
            </button>
        </div>
    `;
    container.innerHTML = html;
}

function changePage(newPage) {
    currentPage = newPage;
    loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function viewProductDetail(productId) {
    window.location.href = `./ProductDetail.html?id=${productId}`;
}

async function addToCart(productId) {
    const token = Api.getToken();
    if (!token) {
        showToast('Please sign in to add items to your cart', 'info');
        setTimeout(() => {
            window.location.href = './Login.html';
        }, 1000);
        return;
    }

    try {
        const response = await Api.post('/cart/items', { productId, quantity: 1 });
        if (response && response.data) {
            showToast('Item added to your cart!', 'success');
            updateCartBadge();
        }
    } catch (error) {
        showToast(error.message || 'Failed to add item to cart', 'error');
    }
}

window.filterByCategory = filterByCategory;
window.changePage = changePage;
window.viewProductDetail = viewProductDetail;
window.addToCart = addToCart;
