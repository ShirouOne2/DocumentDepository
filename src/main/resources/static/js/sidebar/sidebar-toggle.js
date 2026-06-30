document.addEventListener("DOMContentLoaded", function () {

    const toggle = document.getElementById('header-toggle');
    const nav = document.getElementById('nav-bar');
    const bodypd = document.getElementById('body-pd');
    const headerpd = document.getElementById('header');

    if (!toggle || !nav || !bodypd || !headerpd) return;

    const savedState = localStorage.getItem('sidebarState');

    if (savedState === 'open') {
        nav.classList.add('show');
        bodypd.classList.add('body-pd');
        headerpd.classList.add('body-pd');
        toggle.classList.add('bx-x');
    } else {
        nav.classList.remove('show');
        bodypd.classList.remove('body-pd');
        headerpd.classList.remove('body-pd');
        toggle.classList.remove('bx-x');
    }

    toggle.addEventListener('click', () => {

        const isOpen = nav.classList.toggle('show');

        bodypd.classList.toggle('body-pd');
        headerpd.classList.toggle('body-pd');
        toggle.classList.toggle('bx-x');

        localStorage.setItem(
            'sidebarState',
            isOpen ? 'open' : 'collapsed'
        );
    });
});

function toggleDevToolsDropdown(event) {
    event.preventDefault();
    const dropdown = document.querySelector('.nav_dropdown');
    dropdown.classList.toggle('open');
    
    if (dropdown.classList.contains('open')) {
        localStorage.setItem('devtoolsDropdownOpen', 'true');
    } else {
        localStorage.setItem('devtoolsDropdownOpen', 'false');
    }
}

function initDevToolsDropdown() {
    const isOpen = localStorage.getItem('devtoolsDropdownOpen') === 'true';
    const dropdown = document.querySelector('.nav_dropdown');
    
    if (dropdown) {
        if (isOpen) {
            dropdown.classList.add('open');
        }
        
        const activePage = dropdown.querySelector('.nav_sublink.active');
        if (activePage) {
            dropdown.classList.add('open');
        }
    }
}

document.addEventListener('DOMContentLoaded', function() {
    initDevToolsDropdown();
});
