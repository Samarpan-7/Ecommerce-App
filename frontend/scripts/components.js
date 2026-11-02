/**
 * DEATH MART - Reusable Components & UI Helpers
 * Toast notifications, shared navbar, footer, and dynamic cart badges
 */

function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';

    const iconElement = document.createElement('span');
    iconElement.textContent = icon;
    const messageElement = document.createElement('span');
    messageElement.textContent = message;
    toast.append(iconElement, messageElement);
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(50px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, character => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[character]);
}

function safeImageUrl(value) {
    const fallback = 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600';
    const imageUrl = String(value ?? '').trim();
    if (!imageUrl) return fallback;

    try {
        if (imageUrl.startsWith('/uploads/products/')) {
            const apiOrigin = `http://${window.location.hostname || 'localhost'}:8080`;
            return new URL(imageUrl, apiOrigin).href;
        }
        const resolvedUrl = new URL(imageUrl, document.baseURI);
        if (resolvedUrl.protocol === 'http:' || resolvedUrl.protocol === 'https:') {
            return resolvedUrl.href.replace(/['()\\]/g, character =>
                `%${character.charCodeAt(0).toString(16).toUpperCase()}`
            );
        }
        if (!/^[a-z][a-z\d+.-]*:/i.test(imageUrl) &&
            (imageUrl.startsWith('/') || imageUrl.startsWith('./') || imageUrl.startsWith('../'))) {
            return imageUrl;
        }
    } catch (error) {
        console.warn('Ignoring invalid product image URL');
    }

    return fallback;
}

async function updateCartBadge() {
    const badge = document.getElementById('nav-cart-badge');
    if (!badge) return;

    const token = Api.getToken();
    if (!token) {
        badge.textContent = '0';
        return;
    }

    try {
        const response = await Api.get('/cart');
        if (response && response.data) {
            badge.textContent = response.data.totalQuantity || '0';
        }
    } catch (error) {
        console.error('Unable to update cart badge:', error);
    }
}

function renderHeader(activePage = '') {
    const header = document.querySelector('header.site-header') || document.querySelector('header');
    if (!header) return;

    header.className = 'site-header';

    const user = Api.getCurrentUser();
    const isLoggedIn = !!user && !!Api.getToken();
    const isAdmin = isLoggedIn && user.roles && user.roles.includes('ROLE_ADMIN');

    header.innerHTML = `
        <div class="header-container">
            <a href="./Index.html" class="brand-logo">
                <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTzSdFnao4Si8fzlM5IozWw6VlbcCylDT4Oln2I565WpQ&s=10" alt="Death Mart Logo">
                <span class="brand-name">DEATH <span>MART</span></span>
            </a>

            <nav class="main-nav">
                <ul class="nav-links">
                    <li class="nav-item ${activePage === 'home' ? 'active' : ''}"><a href="./Index.html">Home</a></li>
                    <li class="nav-item ${activePage === 'products' ? 'active' : ''}"><a href="./Products.html">Products</a></li>
                    <li class="nav-item ${activePage === 'about' ? 'active' : ''}"><a href="./About.html">About</a></li>
                    <li class="nav-item ${activePage === 'contact' ? 'active' : ''}"><a href="./Contact.html">Contact</a></li>
                    ${isLoggedIn ? `
                        <li class="nav-item ${activePage === 'orders' ? 'active' : ''}"><a href="./Orders.html">My Orders</a></li>
                    ` : ''}
                    ${isAdmin ? `
                        <li class="nav-item ${activePage === 'admin' ? 'active' : ''}"><a href="./Admin.html" style="color: #ff4d4f; font-weight: bold;">⚡ Admin</a></li>
                    ` : ''}
                </ul>

                <div class="nav-actions">
                    <a href="./Cart.html" class="cart-btn-link" title="Shopping Cart">
                        🛒 Cart <span id="nav-cart-badge" class="cart-badge">0</span>
                    </a>

                    ${isLoggedIn ? `
                        <div class="user-menu-btn">
                            <span>👤 ${escapeHtml(user.fullName || user.email)}</span>
                            <button id="logout-btn" class="btn btn-sm btn-secondary" style="margin-left: 6px;">Logout</button>
                        </div>
                    ` : `
                        <a href="./Login.html" class="auth-btn">Sign In</a>
                    `}
                </div>
            </nav>
        </div>
    `;

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            Api.removeToken();
            showToast('Logged out successfully', 'info');
            setTimeout(() => {
                window.location.href = './Login.html';
            }, 600);
        });
    }

    updateCartBadge();
}

function renderFooter() {
    let footer = document.querySelector('footer.site-footer') || document.querySelector('footer');
    if (!footer) {
        footer = document.createElement('footer');
        document.body.appendChild(footer);
    }

    footer.className = 'site-footer';
    footer.innerHTML = `
        <div class="container">
            <div class="footer-grid">
                <div class="footer-brand">
                    <div class="brand-logo">
                        <img src="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTzSdFnao4Si8fzlM5IozWw6VlbcCylDT4Oln2I565WpQ&s=10" alt="Death Mart Logo" style="width: 36px; height: 36px; border-radius: 50%;">
                        <span class="brand-name" style="font-size: 22px;">DEATH <span>MART</span></span>
                    </div>
                    <p>The premier marketplace for Shinigami artifacts, cursed notebooks, detective essentials, and gothic anime collectibles.</p>
                </div>

                <div class="footer-col">
                    <h4>Explore</h4>
                    <ul class="footer-links">
                        <li><a href="./Products.html">All Products</a></li>
                        <li><a href="./Products.html?category=1">Death Note Essentials</a></li>
                        <li><a href="./Products.html?category=5">Shinigami Cafe</a></li>
                        <li><a href="./About.html">Our Backstory</a></li>
                    </ul>
                </div>

                <div class="footer-col">
                    <h4>Customer Care</h4>
                    <ul class="footer-links">
                        <li><a href="./Cart.html">Shopping Cart</a></li>
                        <li><a href="./Orders.html">Track Orders</a></li>
                        <li><a href="./Contact.html">Contact Us / FAQ</a></li>
                        <li><a href="./Login.html">My Account</a></li>
                    </ul>
                </div>

            </div>

            <div class="footer-bottom">
                <p>&copy; ${new Date().getFullYear()} Death Mart Inc. All rights reserved. Built with Java Spring Boot & Modern Web.</p>
            </div>
        </div>
    `;
}

window.showToast = showToast;
window.escapeHtml = escapeHtml;
window.safeImageUrl = safeImageUrl;
window.renderHeader = renderHeader;
window.renderFooter = renderFooter;
window.updateCartBadge = updateCartBadge;
