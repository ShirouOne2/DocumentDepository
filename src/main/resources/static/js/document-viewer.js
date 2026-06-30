/**
 * Document Management System - Consolidated JavaScript
 * Combines all document viewing, editing, commenting, versioning, and export functionality
 */

// ============================================================================
// GLOBAL VARIABLES
// ============================================================================

let currentDocumentId = null;
let currentUserIdGlobal = null;
let isEditMode = false;
let originalDocumentData = {};

// ============================================================================
// DOCUMENT MODAL FUNCTIONS
// ============================================================================

/**
 * Open document viewer modal and load document details
 */
function openDocumentModal(documentId) {
    currentDocumentId = documentId;
    document.getElementById('documentViewerModal').style.display = 'block';
    
    fetch(`/documents/details/${documentId}`)
        .then(response => {
            if (!response.ok) throw new Error('Failed to load document');
            return response.json();
        })
        .then(data => {
            // Populate view mode fields
            document.getElementById('modalDocTitle').textContent = data.title;
            document.getElementById('infoFilename').textContent = data.filename;
            document.getElementById('infoUploadedBy').textContent = data.uploadedBy;
            document.getElementById('infoUploadDate').textContent = formatDate(data.uploadDate);
            document.getElementById('infoClassification').textContent = data.documentClassification;
            document.getElementById('infoViewerGroup').textContent = data.intendedViewerGroup;
            document.getElementById('infoDescription').textContent = data.description || 'No description';
            
            // Populate tags
            const tagsDiv = document.getElementById('infoTags');
            if (data.tags && data.tags.length > 0) {
                tagsDiv.innerHTML = data.tags.map(tag => 
                    `<span class="badge" 
                        style="background-color: #fcebea; 
                                color: #E8392F; 
                                font-size: 0.75rem; 
                                margin-right: 0.25rem;">
                        <i class="fas fa-tag"></i> ${escapeHtml(tag.tagName)}
                    </span>`
                ).join('');
            } else {
                tagsDiv.textContent = 'No tags';
            }
            
            // Populate edit fields
            populateEditFields(data);
            
            // Load comments and version history
            loadComments(data.comments, data.currentUserId);
            document.getElementById('commentCount').textContent = data.commentCount;
            loadVersionHistory(documentId);
            
            // Reset edit mode when opening modal
            cancelEditMode();
        })
        .catch(error => {
            console.error('Error loading document:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'Failed to load document details',
                confirmButtonColor: '#E8392F'
            });
            closeDocumentModal();
        });
}

/**
 * Close document viewer modal
 */
function closeDocumentModal() {
    document.getElementById('documentViewerModal').style.display = 'none';
    currentDocumentId = null;
    
    // Reset edit mode if active
    if (isEditMode) {
        cancelEditMode();
    }
}

/**
 * View document in new tab
 */
function viewDocumentNewTab() {
    if (!currentDocumentId) return;
    window.open(`/documents/view/${currentDocumentId}`, '_blank');
}

/**
 * Download document
 */
function downloadDocument() {
    if (!currentDocumentId) return;
    
    fetch(`/documents/download/${currentDocumentId}`)
        .then(response => response.blob())
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = document.getElementById('infoFilename').textContent || 'document';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        })
        .catch(error => {
            console.error('Download failed:', error);
            Swal.fire({
                icon: 'error',
                title: 'Download Failed',
                text: 'Could not download the file. Please try again.',
                confirmButtonColor: '#E8392F'
            });
        });
}

/**
 * Archive document
 */
function archiveDocument() {
    if (!currentDocumentId) return;
    
    Swal.fire({
        title: 'Archive this document?',
        html: `
            <p>This document will be moved to the archive and will no longer be visible in your active documents.</p>
            <p><strong>Note:</strong> Archived documents can be restored by administrators if needed.</p>
        `,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ff9800',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-archive me-2"></i>Yes, archive it',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/documents/archive/${currentDocumentId}`, {
                method: 'POST'
            })
            .then(response => {
                if (!response.ok) throw new Error('Failed to archive document');
                return response.json();
            })
            .then(data => {
                Swal.fire({
                    icon: 'success',
                    title: 'Archived!',
                    text: 'Document has been archived successfully.',
                    confirmButtonColor: '#E8392F',
                    timer: 2000,
                    showConfirmButton: false
                }).then(() => {
                    window.location.reload();
                });
            })
            .catch(error => {
                console.error('Error archiving document:', error);
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'Failed to archive document. Please try again.',
                    confirmButtonColor: '#E8392F'
                });
            });
        }
    });
}

// ============================================================================
// COMMENT FUNCTIONS
// ============================================================================

/**
 * Load and display comments
 */
function loadComments(comments, currentUserId) {
    const commentsList = document.getElementById('commentsList');
    commentsList.innerHTML = '';
    currentUserIdGlobal = currentUserId;

    if (!comments || comments.length === 0) {
        commentsList.innerHTML = '<div class="no-comments">No comments yet. Be the first to comment!</div>';
        return;
    }

    comments.forEach(comment => {
        const commentDiv = document.createElement('div');
        commentDiv.className = 'comment-item';

        const canDelete = comment.userId && currentUserId === comment.userId;

        commentDiv.innerHTML = `
            <div class="comment-author">
                <i class="fas fa-user-circle"></i>
                ${escapeHtml(comment.username)}
                <span class="comment-date">${formatDate(comment.createdAt)}</span>
                ${canDelete ? `
                    <button class="btn-delete-comment"
                        onclick="deleteComment(${comment.commentId})"
                        title="Delete comment">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                ` : ''}
            </div>
            <div class="comment-text">${escapeHtml(comment.commentText)}</div>
        `;

        commentsList.appendChild(commentDiv);
    });
}

/**
 * Add new comment
 */
function addComment() {
    const commentText = document.getElementById('commentText').value.trim();
    
    if (!commentText) {
        Swal.fire({
            icon: 'warning',
            title: 'Empty Comment',
            text: 'Please enter a comment',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    const formData = new FormData();
    formData.append('commentText', commentText);
    
    fetch(`/documents/${currentDocumentId}/comments`, {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) throw new Error('Failed to add comment');
        return response.json();
    })
    .then(comment => {
        document.getElementById('commentText').value = '';
        Swal.fire({
            icon: 'success',
            title: 'Comment Added',
            text: 'Your comment has been posted',
            confirmButtonColor: '#E8392F',
            timer: 1500,
            showConfirmButton: false
        });
        openDocumentModal(currentDocumentId);
    })
    .catch(error => {
        console.error('Error adding comment:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Failed to add comment. Please try again.',
            confirmButtonColor: '#E8392F'
        });
    });
}

/**
 * Delete comment
 */
function deleteComment(commentId) {
    Swal.fire({
        title: 'Delete this comment?',
        text: "This action cannot be undone.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-trash-alt me-2"></i>Yes, delete it',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch(`/documents/comments/${commentId}`, {
                method: 'DELETE'
            })
            .then(response => {
                if (!response.ok) throw new Error('Failed to delete comment');
                return response.json();
            })
            .then(data => {
                Swal.fire({
                    icon: 'success',
                    title: 'Deleted!',
                    text: 'Comment has been deleted.',
                    confirmButtonColor: '#E8392F',
                    timer: 1500,
                    showConfirmButton: false
                }).then(() => {
                    openDocumentModal(currentDocumentId);
                });
            })
            .catch(error => {
                console.error('Error deleting comment:', error);
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: 'Failed to delete comment. Please try again.',
                    confirmButtonColor: '#E8392F'
                });
            });
        }
    });
}

// ============================================================================
// DOCUMENT EDIT FUNCTIONS
// ============================================================================

/**
 * Toggle edit mode
 */
function toggleEditMode() {
    isEditMode = true;
    
    // Store original values for cancellation
    originalDocumentData = {
        classificationId: document.getElementById('editClassification').value,
        tags: document.getElementById('editTags').value,
        viewerGroupId: document.getElementById('editViewerGroup').value,
        customGroupId: document.getElementById('editCustomGroup') ? document.getElementById('editCustomGroup').value : '',
        description: document.getElementById('editDescription').value
    };
    
    // Show edit controls, hide view controls
    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'block');
    
    // Toggle buttons
    document.getElementById('btnEditMode').style.display = 'none';
    document.getElementById('btnSaveChanges').style.display = 'inline-block';
    document.getElementById('btnCancelEdit').style.display = 'inline-block';
    
    // Show custom group container if currently set to custom group
    const viewerGroupId = parseInt(document.getElementById('editViewerGroup').value);
    if (viewerGroupId === 5) {
        document.getElementById('editCustomGroupContainer').style.display = 'block';
    }
}

/**
 * Cancel edit mode
 */
function cancelEditMode() {
    isEditMode = false;
    
    // Restore original values
    document.getElementById('editClassification').value = originalDocumentData.classificationId || '';
    document.getElementById('editTags').value = originalDocumentData.tags || '';
    document.getElementById('editViewerGroup').value = originalDocumentData.viewerGroupId || '1';
    if (document.getElementById('editCustomGroup')) {
        document.getElementById('editCustomGroup').value = originalDocumentData.customGroupId || '';
    }
    document.getElementById('editDescription').value = originalDocumentData.description || '';
    
    // Show view controls, hide edit controls
    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'block');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'none');
    
    // Toggle buttons
    document.getElementById('btnEditMode').style.display = 'inline-block';
    document.getElementById('btnSaveChanges').style.display = 'none';
    document.getElementById('btnCancelEdit').style.display = 'none';
    
    // Hide custom group container
    document.getElementById('editCustomGroupContainer').style.display = 'none';
}

/**
 * Toggle custom group visibility
 */
function toggleCustomGroupEdit() {
    const viewerGroupId = parseInt(document.getElementById('editViewerGroup').value);
    const customGroupContainer = document.getElementById('editCustomGroupContainer');
    
    if (viewerGroupId === 5) {
        customGroupContainer.style.display = 'block';
    } else {
        customGroupContainer.style.display = 'none';
        if (document.getElementById('editCustomGroup')) {
            document.getElementById('editCustomGroup').value = '';
        }
    }
}

/**
 * Save document changes
 */
function saveDocumentChanges() {
    const classificationId = document.getElementById('editClassification').value;
    const tags = document.getElementById('editTags').value;
    const viewerGroupId = document.getElementById('editViewerGroup').value;
    const customGroupId = document.getElementById('editCustomGroup') ? document.getElementById('editCustomGroup').value : '';
    const description = document.getElementById('editDescription').value;
    
    // Validation
    if (!classificationId) {
        Swal.fire({
            icon: 'warning',
            title: 'Missing Classification',
            text: 'Please select a classification',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    if (viewerGroupId === '5' && !customGroupId) {
        Swal.fire({
            icon: 'warning',
            title: 'Missing Custom Group',
            text: 'Please select a custom group when "Custom Group" is selected',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    // Confirm save
    Swal.fire({
        title: 'Save Changes?',
        text: 'Are you sure you want to update this document?',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-save me-2"></i>Yes, save changes',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            performDocumentUpdate(classificationId, tags, viewerGroupId, customGroupId, description);
        }
    });
}

/**
 * Perform document update
 */
function performDocumentUpdate(classificationId, tags, viewerGroupId, customGroupId, description) {
    // Show loading
    Swal.fire({
        title: 'Saving...',
        text: 'Please wait while we update the document',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
    
    const formData = new FormData();
    formData.append('classificationId', classificationId);
    formData.append('tags', tags);
    formData.append('viewerGroupId', viewerGroupId);
    if (customGroupId) {
        formData.append('customGroupId', customGroupId);
    }
    formData.append('description', description);
    
    fetch(`/documents/${currentDocumentId}/update`, {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.message || 'Failed to update document');
            });
        }
        return response.json();
    })
    .then(data => {
        Swal.fire({
            icon: 'success',
            title: 'Updated!',
            text: 'Document has been updated successfully',
            confirmButtonColor: '#E8392F',
            timer: 2000,
            showConfirmButton: false
        }).then(() => {
            window.location.reload();
        });
    })
    .catch(error => {
        console.error('Error updating document:', error);
        Swal.fire({
            icon: 'error',
            title: 'Update Failed',
            text: error.message || 'Failed to update document. Please try again.',
            confirmButtonColor: '#E8392F'
        });
    });
}

/**
 * Populate edit fields when document modal opens
 */
function populateEditFields(data) {
    // Populate classification
    if (document.getElementById('editClassification')) {
        document.getElementById('editClassification').value = data.classificationId || '';
    }
    
    // Populate tags
    if (document.getElementById('editTags')) {
        const tagsString = data.tags && data.tags.length > 0 
            ? data.tags.map(t => t.tagName).join(', ') 
            : '';
        document.getElementById('editTags').value = tagsString;
    }
    
    // Populate viewer group
    if (document.getElementById('editViewerGroup')) {
        document.getElementById('editViewerGroup').value = data.viewerGroupId || '1';
    }
    
    // Populate custom group
    if (document.getElementById('editCustomGroup')) {
        document.getElementById('editCustomGroup').value = data.customGroupId || '';
    }
    
    // Populate description
    if (document.getElementById('editDescription')) {
        document.getElementById('editDescription').value = data.description || '';
    }
    
    // Show/hide custom group container based on viewer group
    if (data.viewerGroupId === 5 && document.getElementById('editCustomGroupContainer')) {
        document.getElementById('editCustomGroupContainer').style.display = 'block';
    } else if (document.getElementById('editCustomGroupContainer')) {
        document.getElementById('editCustomGroupContainer').style.display = 'none';
    }
}

// ============================================================================
// VERSION HISTORY FUNCTIONS
// ============================================================================

/**
 * Load version history
 */
function loadVersionHistory(documentId) {
    fetch(`/documents/${documentId}/versions`)
        .then(res => res.json())
        .then(versions => {
            const list = document.getElementById('versionHistoryList');
            document.getElementById('versionCount').textContent = versions.length;

            if (!versions.length) {
                list.innerHTML = `<p class="text-muted text-center">No previous versions</p>`;
                return;
            }

            list.innerHTML = versions.map(v => `
                <div class="version-item">
                    <div>
                        <span class="version-badge">v${v.versionNumber}</span>
                        ${v.isArchived ? '<span class="badge bg-secondary ms-2">Archived</span>' : ''}
                    </div>
                    <div class="mt-1">
                        <strong>${escapeHtml(v.originalFilename)}</strong><br>
                        <small class="text-muted">
                            Uploaded by ${escapeHtml(v.uploadedBy)}<br>
                            ${formatDate(v.uploadDate)}
                            ${v.versionNotes ? `<br>Notes: ${escapeHtml(v.versionNotes)}` : ''}
                        </small>
                    </div>
                    <div class="mt-2">
                        <button class="btn btn-sm btn-outline-primary"
                                onclick="viewVersion(${v.documentId})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-secondary"
                                onclick="downloadVersion(${v.documentId})">
                            <i class="fas fa-download"></i>
                        </button>
                    </div>
                </div>
            `).join('');
        })
        .catch(error => {
            console.error('Error loading version history:', error);
        });
}

/**
 * View a specific version
 */
function viewVersion(documentId) {
    window.open(`/documents/view/${documentId}`, '_blank');
}

/**
 * Download a specific version
 */
function downloadVersion(documentId) {
    window.open(`/documents/download/${documentId}`, '_blank');
}

/**
 * Open the upload version modal
 */
function openUploadVersionModal() {
    if (!currentDocumentId) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No document selected',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    document.getElementById('versionDocumentId').value = currentDocumentId;
    document.getElementById('uploadVersionModal').style.display = 'block';
}

/**
 * Close the upload version modal
 */
function closeUploadVersionModal() {
    document.getElementById('uploadVersionModal').style.display = 'none';
    document.getElementById('uploadVersionForm').reset();
}

/**
 * Confirm and upload new version
 */
function confirmAndUploadVersion(documentId, file, notes) {
    const fileName = file.name;
    const fileSize = formatFileSize(file.size);
    
    Swal.fire({
        title: 'Upload New Version?',
        html: `
            <div class="text-start">
                <p><strong>File:</strong> ${escapeHtml(fileName)}</p>
                <p><strong>Size:</strong> ${fileSize}</p>
                ${notes ? `<p><strong>Notes:</strong> ${escapeHtml(notes)}</p>` : ''}
                <hr>
                <p class="text-warning">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    The current file will be archived and replaced with the new version.
                </p>
                <p class="text-muted"><strong>This action cannot be undone.</strong></p>
            </div>
        `,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#E8392F',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-upload me-2"></i>Yes, upload new version',
        cancelButtonText: 'Cancel',
        customClass: {
            popup: 'swal-wide'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            uploadNewVersion(documentId, file, notes);
        }
    });
}

/**
 * Perform the version upload
 */
function uploadNewVersion(documentId, file, notes) {
    // Show loading
    Swal.fire({
        title: 'Uploading...',
        html: `
            <div class="upload-progress">
                <p>Please wait while we upload the new version</p>
                <div class="progress mt-3">
                    <div class="progress-bar progress-bar-striped progress-bar-animated" 
                         role="progressbar" 
                         style="width: 100%"></div>
                </div>
            </div>
        `,
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
    
    const formData = new FormData();
    formData.append('file', file);
    if (notes) {
        formData.append('versionNotes', notes);
    }
    
    fetch(`/documents/${documentId}/upload-new-version`, {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(err => {
                throw new Error(err.message || 'Upload failed');
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.status === 'success') {
            Swal.fire({
                icon: 'success',
                title: 'Success!',
                html: `
                    <p>Version ${data.versionNumber} uploaded successfully!</p>
                    <p class="text-muted">The previous version has been archived.</p>
                `,
                confirmButtonColor: '#E8392F',
                timer: 3000
            }).then(() => {
                closeUploadVersionModal();
                window.location.reload();
            });
        } else {
            throw new Error(data.message || 'Upload failed');
        }
    })
    .catch(error => {
        console.error('Upload error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Upload Failed',
            html: `
                <p>${escapeHtml(error.message)}</p>
                <p class="text-muted mt-2">Please try again or contact support if the problem persists.</p>
            `,
            confirmButtonColor: '#E8392F'
        });
    });
}

// ============================================================================
// EXPORT FUNCTIONS
// ============================================================================

/**
 * Confirm and trigger Excel export
 * Note: totalItems, query, etc. are passed from Thymeleaf inline script in HTML
 */
function confirmExport() {
    // These variables should be defined in an inline script tag in the HTML
    // using Thymeleaf to inject the actual values
    if (typeof totalItems === 'undefined') {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Export data not available',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    // Build filter summary
    const filters = buildFilterSummary(
        typeof query !== 'undefined' ? query : '',
        typeof classificationId !== 'undefined' ? classificationId : null,
        typeof startDate !== 'undefined' ? startDate : '',
        typeof endDate !== 'undefined' ? endDate : ''
    );
    
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
            performExport();
        }
    });
}

/**
 * Perform the Excel export
 * Uses variables defined in inline Thymeleaf script
 */
function performExport() {
    // Get values from Thymeleaf inline script or URL params as fallback
    const _totalItems = typeof totalItems !== 'undefined' ? totalItems : 0;
    const _query = typeof query !== 'undefined' ? query : '';
    const _classificationId = typeof classificationId !== 'undefined' ? classificationId : null;
    const _startDate = typeof startDate !== 'undefined' ? startDate : '';
    const _endDate = typeof endDate !== 'undefined' ? endDate : '';
    
    // Show loading
    Swal.fire({
        title: 'Generating Excel...',
        html: `
            <div class="export-progress">
                <p>Preparing ${_totalItems} records for export...</p>
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
    
    if (_query) params.append('query', _query);
    if (_classificationId) params.append('classificationId', _classificationId);
    if (_startDate) params.append('startDate', _startDate);
    if (_endDate) params.append('endDate', _endDate);
    
    // Determine the correct export endpoint based on current page
    const currentPage = getCurrentPage();
    const exportUrl = `${currentPage}/export?${params.toString()}`;
    
    // Trigger download
    window.location.href = exportUrl;
    
    // Close loading after a delay
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
 * Get current page path
 */
function getCurrentPage() {
    const path = window.location.pathname;
    
    if (path.includes('/myfiles')) {
        return '/myfiles';
    } else if (path.includes('/shared')) {
        return '/shared';
    }
    
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

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Format date for display
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Format file size for display
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Format number with thousands separator
 */
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// ============================================================================
// EVENT LISTENERS
// ============================================================================

document.addEventListener('DOMContentLoaded', function() {
    // Attach click handlers to view buttons
    document.querySelectorAll('.btn-view').forEach(btn => {
        btn.addEventListener('click', function () {
            const id = this.dataset.id;
            openDocumentModal(id);
        });
    });
    
    // Version upload form handler
    const uploadForm = document.getElementById('uploadVersionForm');
    if (uploadForm) {
        uploadForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const documentId = document.getElementById('versionDocumentId').value;
            const file = document.getElementById('versionFile').files[0];
            const notes = document.getElementById('versionNotes').value;
            
            if (!file) {
                Swal.fire({
                    icon: 'warning',
                    title: 'No File Selected',
                    text: 'Please select a file to upload',
                    confirmButtonColor: '#E8392F'
                });
                return;
            }
            
            // Validate file size (50MB max)
            const maxSize = 50 * 1024 * 1024;
            if (file.size > maxSize) {
                Swal.fire({
                    icon: 'warning',
                    title: 'File Too Large',
                    text: 'File size must be less than 50MB',
                    confirmButtonColor: '#E8392F'
                });
                return;
            }
            
            confirmAndUploadVersion(documentId, file, notes);
        });
    }
    
    // File input preview
    const fileInput = document.getElementById('versionFile');
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                console.log('Selected file:', file.name, formatFileSize(file.size));
            }
        });
    }
    
    // Add data attribute to total items display
    const totalItemsDisplay = document.querySelector('.total-items, [class*="totalItems"]');
    if (totalItemsDisplay) {
        const text = totalItemsDisplay.textContent;
        const match = text.match(/\d+/);
        if (match) {
            totalItemsDisplay.setAttribute('data-total-items', match[0]);
        }
    }
});

// Close modals when clicking outside
window.onclick = function(event) {
    const documentModal = document.getElementById('documentViewerModal');
    const uploadModal = document.getElementById('uploadVersionModal');
    
    if (event.target === documentModal) {
        closeDocumentModal();
    }
    if (event.target === uploadModal) {
        closeUploadVersionModal();
    }
}

// Close modals with Escape key
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        const documentModal = document.getElementById('documentViewerModal');
        const uploadModal = document.getElementById('uploadVersionModal');
        
        if (documentModal && documentModal.style.display === 'block') {
            closeDocumentModal();
        }
        if (uploadModal && uploadModal.style.display === 'block') {
            closeUploadVersionModal();
        }
    }
});