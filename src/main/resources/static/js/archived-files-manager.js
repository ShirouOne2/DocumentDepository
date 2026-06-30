/**
 * Archived Files Manager
 * Handles viewing, downloading, and restoring archived documents
 * Updated to support broken link detection
 */

let currentDocumentId = null;
let currentDocumentTitle = null;
let currentDocumentOwnerId = null;
let currentUserId = null;
let currentDocumentIsBroken = false;

// Initialize event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Attach view button listeners
    document.querySelectorAll('.btn-view').forEach(button => {
        button.addEventListener('click', function() {
            const documentId = this.getAttribute('data-id');
            openDocumentModal(documentId);
        });
    });

    // Attach restore button listeners
    document.querySelectorAll('.btn-restore').forEach(button => {
        button.addEventListener('click', function() {
            const documentId = this.getAttribute('data-id');
            const documentTitle = this.getAttribute('data-title');
            const isBroken = this.getAttribute('data-broken') === 'true';
            
            if (isBroken) {
                Swal.fire({
                    icon: 'error',
                    title: 'Cannot Restore',
                    text: 'This document cannot be restored because the file is missing from storage.',
                    confirmButtonColor: '#dc3545'
                });
                return;
            }
            
            confirmRestore(documentId, documentTitle);
        });
    });
});

/**
 * Open document viewer modal
 */
async function openDocumentModal(documentId) {
    currentDocumentId = documentId;
    
    try {
        // Fetch document details
        const response = await fetch(`/documents/details/${documentId}`);
        const data = await response.json();
        
        // Check if file exists
        const fileExistsResponse = await fetch(`/api/documents/${documentId}/file-exists`);
        const fileExistsData = await fileExistsResponse.json();
        currentDocumentIsBroken = !fileExistsData.exists;
        
        // Store document info for restore
        currentDocumentTitle = data.title;
        currentDocumentOwnerId = data.uploadedBy;
        currentUserId = data.currentUserId;
        
        // Populate modal
        document.getElementById('modalDocTitle').textContent = data.title;
        
        // Show/hide broken link badge and alert
        const brokenBadge = document.getElementById('modalBrokenBadge');
        const brokenAlert = document.getElementById('brokenLinkAlert');
        const btnView = document.getElementById('btnViewModal');
        const btnDownload = document.getElementById('btnDownloadModal');
        const btnRestore = document.getElementById('btnRestoreModal');
        
        if (currentDocumentIsBroken) {
            if (brokenBadge) brokenBadge.style.display = 'inline-block';
            if (brokenAlert) brokenAlert.style.display = 'block';
            if (btnView) btnView.disabled = true;
            if (btnDownload) btnDownload.disabled = true;
        } else {
            if (brokenBadge) brokenBadge.style.display = 'none';
            if (brokenAlert) brokenAlert.style.display = 'none';
            if (btnView) btnView.disabled = false;
            if (btnDownload) btnDownload.disabled = false;
        }
        
        // File status
        const fileStatusElem = document.getElementById('infoFileStatus');
        if (fileStatusElem) {
            fileStatusElem.innerHTML = !currentDocumentIsBroken 
                ? '<span class="badge bg-success"><i class="fas fa-check"></i> OK</span>'
                : '<span class="badge bg-danger"><i class="fas fa-unlink"></i> Broken Link</span>';
        }
        
        document.getElementById('infoFilename').textContent = data.filename;
        document.getElementById('infoUploadedBy').textContent = data.uploadedBy;
        document.getElementById('infoUploadDate').textContent = formatDateTime(data.uploadDate);
        document.getElementById('infoArchivedDate').textContent = formatDateTime(data.dateCreated);
        document.getElementById('infoArchivedBy').textContent = data.uploadedBy; // Adjust if you have archivedBy field
        document.getElementById('infoClassification').textContent = data.documentClassification;
        document.getElementById('infoViewerGroup').textContent = data.intendedViewerGroup;
        document.getElementById('infoDescription').textContent = data.description || 'No description';
        
        // Display tags
        const tagsContainer = document.getElementById('infoTags');
        if (data.tags && data.tags.length > 0) {
            tagsContainer.innerHTML = data.tags.map(tag => 
                `<span class="badge" style="background-color: #fcebea; color: #E8392F; font-size: 0.7rem; margin-right: 0.25rem;">
                    <i class="fas fa-tag" style="font-size: 0.6rem;"></i> ${tag}
                </span>`
            ).join('');
        } else {
            tagsContainer.textContent = 'No tags';
        }
        
        // Load comments (read-only)
        loadComments(data.comments);
        
        // Show/hide restore button based on permissions and file status
        if (btnRestore) {
            if (isAdminUser || currentUserId === currentDocumentOwnerId) {
                btnRestore.style.display = 'inline-block';
                btnRestore.disabled = currentDocumentIsBroken; // Disable if file doesn't exist
                if (currentDocumentIsBroken) {
                    btnRestore.title = 'Cannot restore - file not found';
                } else {
                    btnRestore.title = 'Restore document';
                }
            } else {
                btnRestore.style.display = 'none';
            }
        }
        
        // Show modal
        document.getElementById('documentViewerModal').style.display = 'block';
        
    } catch (error) {
        console.error('Error loading document:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Failed to load document details'
        });
    }
}

/**
 * Load comments (read-only for archived documents)
 */
function loadComments(comments) {
    const commentsList = document.getElementById('commentsList');
    const commentCount = document.getElementById('commentCount');
    
    if (!comments || comments.length === 0) {
        commentsList.innerHTML = '<p class="text-muted">No comments</p>';
        commentCount.textContent = '0';
        return;
    }
    
    commentCount.textContent = comments.length;
    
    commentsList.innerHTML = comments.map(comment => `
        <div class="comment-item" style="border-left: 3px solid #6c757d; padding-left: 1rem; margin-bottom: 1rem;">
            <div class="comment-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <strong style="color: #333;">
                    <i class="fas fa-user-circle me-1"></i>
                    ${comment.username}
                </strong>
                <small class="text-muted">
                    <i class="fas fa-clock me-1"></i>
                    ${formatDateTime(comment.createdAt)}
                </small>
            </div>
            <div class="comment-text" style="color: #555;">
                ${escapeHtml(comment.commentText)}
            </div>
        </div>
    `).join('');
}

/**
 * Close document modal
 */
function closeDocumentModal() {
    document.getElementById('documentViewerModal').style.display = 'none';
    currentDocumentId = null;
    currentDocumentIsBroken = false;
}

/**
 * View document in new tab
 */
function viewDocumentNewTab() {
    if (currentDocumentId && !currentDocumentIsBroken) {
        window.open(`/documents/view/${currentDocumentId}`, '_blank');
    }
}

/**
 * Download document
 */
function downloadDocument(docId = null) {
    const documentId = docId || currentDocumentId;
    if (documentId && !currentDocumentIsBroken) {
        window.location.href = `/documents/download/${documentId}`;
    }
}

/**
 * Confirm export to Excel
 */
function confirmExport() {
    if (totalItems === 0) {
        Swal.fire({
            icon: 'warning',
            title: 'No Data',
            text: 'There are no archived documents to export.'
        });
        return;
    }

    Swal.fire({
        title: 'Export to Excel',
        html: `
            <p>Export <strong>${totalItems}</strong> archived document(s) to Excel?</p>
            ${query ? `<p class="text-muted small">Search: "${query}"</p>` : ''}
            <p class="text-info small">
                <i class="fas fa-info-circle"></i>
                The export will include a "File Status" column indicating broken links.
            </p>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-file-excel"></i> Export',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            exportToExcel();
        }
    });
}

/**
 * Export to Excel
 */
function exportToExcel() {
    // Build URL with current filters
    const params = new URLSearchParams();
    
    if (query) params.append('query', query);
    if (classificationId) params.append('classificationId', classificationId);
    if (uploadedById) params.append('uploadedById', uploadedById);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (filterAreaId) params.append('filterAreaId', filterAreaId);
    if (filterOfficeId) params.append('filterOfficeId', filterOfficeId);
    if (filterDepartmentId) params.append('filterDepartmentId', filterDepartmentId);
    if (customGroupId) params.append('customGroupId', customGroupId);
    
    const url = `/archived/export?${params.toString()}`;
    
    Swal.fire({
        title: 'Exporting...',
        html: 'Please wait while we generate your Excel file.',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
    
    // Trigger download
    window.location.href = url;
    
    // Close loading after a delay
    setTimeout(() => {
        Swal.close();
        Swal.fire({
            icon: 'success',
            title: 'Export Complete',
            text: 'Your file has been downloaded.',
            timer: 2000,
            showConfirmButton: false
        });
    }, 1500);
}

/**
 * Format datetime for display
 */
function formatDateTime(dateString) {
    if (!dateString) return '-';
    
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}`;
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Confirm restore from table button
 */
function confirmRestore(documentId, documentTitle) {
    Swal.fire({
        title: 'Restore Document?',
        html: `
            <p>Are you sure you want to restore this document?</p>
            <p class="text-muted"><strong>${escapeHtml(documentTitle)}</strong></p>
            <p class="text-info small">
                <i class="fas fa-info-circle"></i>
                This will move the document back to your active files.
            </p>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-undo"></i> Yes, Restore',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            restoreDocument(documentId, documentTitle);
        }
    });
}

/**
 * Restore document from modal
 */
function restoreDocumentFromModal() {
    if (currentDocumentIsBroken) {
        Swal.fire({
            icon: 'error',
            title: 'Cannot Restore',
            text: 'This document cannot be restored because the file is missing from storage.',
            confirmButtonColor: '#dc3545'
        });
        return;
    }
    
    confirmRestore(currentDocumentId, currentDocumentTitle);
}

/**
 * Restore archived document
 */
async function restoreDocument(documentId, documentTitle) {
    try {
        // Show loading
        Swal.fire({
            title: 'Restoring...',
            html: 'Please wait while we restore the document.',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        const response = await fetch(`/archived/restore/${documentId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();

        if (data.success) {
            Swal.fire({
                icon: 'success',
                title: 'Restored!',
                html: `
                    <p><strong>${escapeHtml(documentTitle)}</strong> has been restored.</p>
                    <p class="text-muted small">The document is now available in your active files.</p>
                `,
                confirmButtonColor: '#198754',
                confirmButtonText: 'OK'
            }).then(() => {
                // Reload the page to update the list
                window.location.reload();
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Restore Failed',
                text: data.message || 'Failed to restore document',
                confirmButtonColor: '#dc3545'
            });
        }

    } catch (error) {
        console.error('Error restoring document:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'An error occurred while restoring the document',
            confirmButtonColor: '#dc3545'
        });
    }
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('documentViewerModal');
    if (event.target === modal) {
        closeDocumentModal();
    }
};