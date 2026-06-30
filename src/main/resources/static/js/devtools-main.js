/**
 * DevTools Main Initialization
 * Coordinates all DevTools modules
 */

document.addEventListener('DOMContentLoaded', function() {
    initializeAlerts();
    initializeTabs();
    
    // Initialize all modules
    if (typeof DevToolsUsers !== 'undefined') DevToolsUsers.init();
    if (typeof DevToolsPositions !== 'undefined') DevToolsPositions.init();
    if (typeof DevToolsDepartments !== 'undefined') DevToolsDepartments.init();
    if (typeof DevToolsAreas !== 'undefined') DevToolsAreas.init();
    if (typeof DevToolsOffices !== 'undefined') DevToolsOffices.init();
    if (typeof DevToolsViewerGroups !== 'undefined') DevToolsViewerGroups.init();
    if (typeof DevToolsClassifications !== 'undefined') DevToolsClassifications.init();

    // NOTE: Modals intentionally do NOT close on outside click or Escape key.
    // Close buttons inside each modal are the only way to dismiss them.
});

/**
 * Tab switching logic
 */
function initializeTabs() {
    const tabs = document.querySelectorAll('.devtools-tab');
    const panels = document.querySelectorAll('.devtools-panel');

    if (!tabs.length) return;

    // Restore last active tab from sessionStorage
    const savedTab = sessionStorage.getItem('devtools-active-tab');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const target = tab.dataset.tab;

            tabs.forEach(t => t.classList.remove('active'));
            panels.forEach(p => p.classList.remove('active'));

            tab.classList.add('active');
            const panel = document.getElementById('panel-' + target);
            if (panel) panel.classList.add('active');

            sessionStorage.setItem('devtools-active-tab', target);
        });

        // Restore saved tab on load
        if (savedTab && tab.dataset.tab === savedTab) {
            tab.click();
        }
    });

    // If nothing restored, activate first tab
    if (!savedTab || !document.querySelector('.devtools-tab.active')) {
        tabs[0]?.click();
    }
}

/**
 * Display flash messages from server
 */
function initializeAlerts() {
    const successMsg = document.getElementById('successMessage')?.textContent;
    const errorMsg = document.getElementById('errorMessage')?.textContent;

    if (successMsg && successMsg.trim()) {
        Swal.fire({
            icon: 'success',
            title: 'Success!',
            text: successMsg,
            confirmButtonColor: '#E8392F',
            timer: 3000,
            timerProgressBar: true
        });
    }

    if (errorMsg && errorMsg.trim()) {
        Swal.fire({
            icon: 'error',
            title: 'Error!',
            text: errorMsg,
            confirmButtonColor: '#E8392F'
        });
    }
}