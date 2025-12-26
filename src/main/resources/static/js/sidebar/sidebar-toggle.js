document.addEventListener("DOMContentLoaded", function () {

    const toggle = document.getElementById('header-toggle');
    const nav = document.getElementById('nav-bar');
    const bodypd = document.getElementById('body-pd');
    const headerpd = document.getElementById('header');

    if (!toggle || !nav || !bodypd || !headerpd) return;

    /* ===============================
       RESTORE STATE (DEFAULT = COLLAPSED)
    =============================== */
    const savedState = localStorage.getItem('sidebarState');

    if (savedState === 'open') {
        nav.classList.add('show');
        bodypd.classList.add('body-pd');
        headerpd.classList.add('body-pd');
        toggle.classList.add('bx-x');
    } else {
        // collapsed by default
        nav.classList.remove('show');
        bodypd.classList.remove('body-pd');
        headerpd.classList.remove('body-pd');
        toggle.classList.remove('bx-x');
    }

    /* ===============================
       TOGGLE SIDEBAR
    =============================== */
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
