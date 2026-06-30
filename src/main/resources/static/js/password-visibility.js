/**
 * Password Visibility Toggle
 * Add this script to your main layout or individual password forms
 */

document.addEventListener('DOMContentLoaded', function() {
    // Find all password inputs
    const passwordInputs = document.querySelectorAll('input[type="password"]');
    
    passwordInputs.forEach(input => {
        // Skip if already has toggle
        if (input.parentElement.querySelector('.password-toggle')) return;
        
        // Create wrapper if input is not already wrapped
        let wrapper = input.parentElement;
        if (!wrapper.classList.contains('password-wrapper')) {
            wrapper = document.createElement('div');
            wrapper.className = 'password-wrapper';
            wrapper.style.position = 'relative';
            input.parentNode.insertBefore(wrapper, input);
            wrapper.appendChild(input);
        }
        
        // Create toggle button
        const toggleBtn = document.createElement('button');
        toggleBtn.type = 'button';
        toggleBtn.className = 'password-toggle';
        toggleBtn.innerHTML = '<i class="bx bx-hide"></i>';
        toggleBtn.setAttribute('aria-label', 'Toggle password visibility');
        
        // Style the button
        Object.assign(toggleBtn.style, {
            position: 'absolute',
            right: '10px',
            top: '50%',
            transform: 'translateY(-50%)',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: '#6c757d',
            fontSize: '1.2rem',
            padding: '0',
            lineHeight: '1',
            zIndex: '10'
        });
        
        // Adjust input padding to prevent text overlap
        input.style.paddingRight = '40px';
        
        // Toggle functionality
        toggleBtn.addEventListener('click', function() {
            const icon = this.querySelector('i');
            
            if (input.type === 'password') {
                input.type = 'text';
                icon.className = 'bx bx-show';
                this.setAttribute('aria-label', 'Hide password');
            } else {
                input.type = 'password';
                icon.className = 'bx bx-hide';
                this.setAttribute('aria-label', 'Show password');
            }
        });
        
        wrapper.appendChild(toggleBtn);
    });
});