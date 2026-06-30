/**
 * Export Handler - Excel Export Functionality
 * Handles exporting search results to Excel
 */

/**
 * Confirm and trigger Excel export
 * This function is called when user clicks the "Export to Excel" button
 */
function confirmExport() {
    // Get current search parameters from the page
    const totalItems = getTotalItems();
    const query = getQueryParam('query');
    const classificationId = getQueryParam('classificationId');
    const startDate = getQueryParam('startDate');
    const endDate = getQueryParam('endDate');
    
    // Build filter summary
    const filters = buildFilterSummary(query, classificationId, startDate, endDate);
    
    Swal.fire({
        title: 'Export to Excel?',
        html: `
            <div class="text-start">
                <p>You are about to export <strong>${totalItems} record(s)</strong> to Excel.</p>
                ${filters ? `<div class="mt-3"><strong>Active Filters:</strong>${filters}</div>` : ''}
                <p class="mt-3">This will download a file with all your filtered documents.</p>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-file-excel me-2"></i>Yes, export it!',
        cancelButtonText: 'Cancel',
        customClass: {
            popup: 'swal-wide'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            performExport(totalItems, query, classificationId, startDate, endDate);
        }
    });
}

/**
 * Perform the Excel export
 */
function performExport(totalItems, query, classificationId, startDate, endDate) {
    // Show loading
    Swal.fire({
        title: 'Generating Excel...',
        html: `
            <div class="export-progress">
                <p>Preparing ${totalItems} records for export...</p>
                <div class="progress mt-3">
                    <div class="progress-bar progress-bar-striped progress-bar-animated" 
                         role="progressbar" 
                         style="width: 100%"></div>
                </div>
                <p class="text-muted mt-2">This may take a moment for large datasets.</p>
            </div>
        `,
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
    
    // Build export URL with current filters
    const params = new URLSearchParams();
    
    if (query) params.append('query', query);
    if (classificationId) params.append('classificationId', classificationId);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    
    // Determine the correct export endpoint based on current page
    const currentPage = getCurrentPage();
    const exportUrl = `${currentPage}/export?${params.toString()}`;
    
    // Trigger download
    window.location.href = exportUrl;
    
    // Close loading after a delay (download should have started)
    setTimeout(() => {
        Swal.fire({
            icon: 'success',
            title: 'Download Started!',
            text: 'Your Excel file is being downloaded.',
            confirmButtonColor: '#28a745',
            timer: 2000,
            showConfirmButton: false
        });
    }, 1000);
}

/**
 * Get total items count from the page
 */
function getTotalItems() {
    // Try to get from Thymeleaf variable first
    const totalItemsElement = document.querySelector('[data-total-items]');
    if (totalItemsElement) {
        return parseInt(totalItemsElement.dataset.totalItems) || 0;
    }
    
    // Fallback: count table rows (excluding header)
    const tableRows = document.querySelectorAll('table tbody tr');
    return tableRows.length;
}

/**
 * Get query parameter from URL or form
 */
function getQueryParam(paramName) {
    // Try URL first
    const urlParams = new URLSearchParams(window.location.search);
    let value = urlParams.get(paramName);
    
    // If not in URL, try to get from form input
    if (!value) {
        const input = document.querySelector(`[name="${paramName}"]`);
        if (input) {
            value = input.value;
        }
    }
    
    return value || '';
}

/**
 * Get current page path (e.g., /myfiles or /shared)
 */
function getCurrentPage() {
    const path = window.location.pathname;
    
    if (path.includes('/myfiles')) {
        return '/myfiles';
    } else if (path.includes('/shared')) {
        return '/shared';
    }
    
    // Default fallback
    return '/myfiles';
}

/**
 * Build filter summary HTML
 */
function buildFilterSummary(query, classificationId, startDate, endDate) {
    const filters = [];
    
    if (query) {
        filters.push(`<li><strong>Search:</strong> "${escapeHtml(query)}"</li>`);
    }
    
    if (classificationId) {
        const classSelect = document.querySelector('[name="classificationId"]');
        const classText = classSelect ? classSelect.options[classSelect.selectedIndex].text : 'Selected';
        filters.push(`<li><strong>Classification:</strong> ${escapeHtml(classText)}</li>`);
    }
    
    if (startDate) {
        filters.push(`<li><strong>From:</strong> ${escapeHtml(startDate)}</li>`);
    }
    
    if (endDate) {
        filters.push(`<li><strong>To:</strong> ${escapeHtml(endDate)}</li>`);
    }
    
    if (filters.length === 0) {
        return '';
    }
    
    return `<ul class="text-start">${filters.join('')}</ul>`;
}

/**
 * Add export button functionality for dynamically created buttons
 */
document.addEventListener('DOMContentLoaded', function() {
    // Find and attach to export buttons
    const exportButtons = document.querySelectorAll('[onclick*="confirmExport"]');
    
    exportButtons.forEach(button => {
        // Already has onclick, no need to add listener
        console.log('Export button found and ready');
    });
    
    // Add data attribute to total items display for easier access
    const totalItemsDisplay = document.querySelector('.total-items, [class*="totalItems"]');
    if (totalItemsDisplay) {
        const text = totalItemsDisplay.textContent;
        const match = text.match(/\d+/);
        if (match) {
            totalItemsDisplay.setAttribute('data-total-items', match[0]);
        }
    }
});

/**
 * Handle export errors
 */
function handleExportError(error) {
    console.error('Export error:', error);
    
    Swal.fire({
        icon: 'error',
        title: 'Export Failed',
        html: `
            <p>Failed to generate Excel file.</p>
            <p class="text-muted mt-2">${escapeHtml(error.message || 'Unknown error')}</p>
            <p class="text-muted">Please try again or contact support if the problem persists.</p>
        `,
        confirmButtonColor: '#E8392F'
    });
}

/**
 * Format number with thousands separator
 */
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

/**
 * Escape HTML helper (reuse if not already defined)
 */
if (typeof escapeHtml === 'undefined') {
    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}