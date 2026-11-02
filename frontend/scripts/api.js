/**
 * DEATH MART - API Client Utility
 * Centralizes all REST API calls to the Spring Boot backend
 */

const API_HOST = window.location.hostname || 'localhost';
const API_BASE_URL = `http://${API_HOST}:8080/api`;

const Api = {
    getToken() {
        return localStorage.getItem('dm_token');
    },

    setToken(token) {
        localStorage.setItem('dm_token', token);
    },

    removeToken() {
        localStorage.removeItem('dm_token');
        localStorage.removeItem('dm_user');
    },

    getCurrentUser() {
        const user = localStorage.getItem('dm_user');
        return user ? JSON.parse(user) : null;
    },

    setCurrentUser(user) {
        localStorage.setItem('dm_user', JSON.stringify(user));
    },

    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = {
            ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);

            // Handle 401 Unauthorized (expired token)
            if (response.status === 401 && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
                this.removeToken();
                if (window.location.pathname.includes('Cart') ||
                    window.location.pathname.includes('Checkout') ||
                    window.location.pathname.includes('Orders') ||
                    window.location.pathname.includes('Profile') ||
                    window.location.pathname.includes('Admin')) {
                    window.location.href = './Login.html?sessionExpired=true';
                }
            }

            const data = await response.json();

            if (!response.ok) {
                const errorMessage = data.message || (data.data && typeof data.data === 'object' ? Object.values(data.data).join(', ') : 'An error occurred');
                throw new Error(errorMessage);
            }

            return data;
        } catch (error) {
            console.error(`API Error on [${options.method || 'GET'}] ${endpoint}:`, error);
            if (error instanceof TypeError && error.message === 'Failed to fetch') {
                throw new Error(`Could not reach ${API_BASE_URL}. Start the backend on port 8080, verify its MySQL connection, and serve the frontend over HTTP.`);
            }
            throw error;
        }
    },

    get(endpoint, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const fullEndpoint = queryString ? `${endpoint}?${queryString}` : endpoint;
        return this.request(fullEndpoint, { method: 'GET' });
    },

    post(endpoint, body = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    upload(endpoint, formData) {
        return this.request(endpoint, {
            method: 'POST',
            body: formData
        });
    },

    put(endpoint, body = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }
};

window.Api = Api;
