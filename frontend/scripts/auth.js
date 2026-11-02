/**
 * DEATH MART - Authentication Handler
 * Handles Login and Registration forms with validation & token persistence
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Handle Login Form
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitBtn = loginForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Authenticating...';

            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;

            try {
                const response = await Api.post('/auth/login', { email, password });
                if (response && response.data) {
                    Api.setToken(response.data.token);
                    Api.setCurrentUser(response.data);
                    showToast('Welcome back, ' + (response.data.fullName || 'Shinobi') + '!', 'success');

                    setTimeout(() => {
                        if (response.data.roles && response.data.roles.includes('ROLE_ADMIN')) {
                            window.location.href = './Admin.html';
                        } else {
                            window.location.href = './Index.html';
                        }
                    }, 800);
                }
            } catch (error) {
                showToast(error.message || 'Login failed. Please check your credentials.', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
        });
    }

    // 2. Handle Register Form
    const registerForm = document.getElementById('register-form');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitBtn = registerForm.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.disabled = true;
            submitBtn.textContent = 'Creating Account...';

            const fullName = document.getElementById('fullName').value.trim();
            const email = document.getElementById('email').value.trim();
            const phone = document.getElementById('phone').value.trim();
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (password !== confirmPassword) {
                showToast('Passwords do not match', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
                return;
            }

            try {
                const response = await Api.post('/auth/register', { fullName, email, phone, password, confirmPassword });
                if (response && response.data) {
                    showToast('Account created. Please sign in.', 'success');
                    setTimeout(() => {
                        window.location.href = './Login.html';
                    }, 600);
                }
            } catch (error) {
                showToast(error.message || 'Registration failed', 'error');
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            }
        });
    }
});
