/**
 * DEATH MART - Contact Form Controller
 * Handles customer inquiries, messages, and feedback submissions
 */

document.addEventListener('DOMContentLoaded', () => {
    const contactForm = document.getElementById('contact-form');
    if (!contactForm) return;

    contactForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = contactForm.querySelector('button[type="submit"]');
        const origText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Transmitting Message...';

        const name = document.getElementById('name').value.trim();
        const email = document.getElementById('email').value.trim();
        const subject = document.getElementById('subject').value.trim();
        const message = document.getElementById('message').value.trim();

        try {
            const response = await Api.post('/contact', { name, email, subject, message });
            showToast('Message sent to the Shinigami realm! We will respond shortly.', 'success');
            contactForm.reset();
        } catch (error) {
            showToast(error.message || 'Failed to send message', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = origText;
        }
    });
});
