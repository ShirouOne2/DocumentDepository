/**
 * Shared Files Document Management System - JavaScript
 * This version has restricted functionality - only admins can edit/upload/archive
 */

// ============================================================================
// GLOBAL VARIABLES
// ============================================================================

let currentDocumentId = null;
let currentUserIdGlobal = null;
let isEditMode = false;
let originalDocumentData = {};
// isAdminUser is set from Thymeleaf template (see inline script in HTML)

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
            
            // Populate tags - FIXED to use tagNames array
            const tagsDiv = document.getElementById('infoTags');
            if (data.tagNames && data.tagNames.length > 0) {
                tagsDiv.innerHTML = data.tagNames.map(tagName => 
                    `<span class="badge" 
                        style="background-color: #fcebea; 
                                color: #E8392F; 
                                font-size: 0.75rem; 
                                margin-right: 0.25rem;">
                        <i class="fas fa-tag"></i> ${escapeHtml(tagName)}
                    </span>`
                ).join('');
            } else {
                tagsDiv.textContent = 'No tags';
            }
            
            // Populate edit fields ONLY if admin
            if (typeof isAdminUser !== 'undefined' && isAdminUser) {
                populateEditFields(data);
            }
            
            // Load comments (everyone can see these)
            loadComments(data.comments, data.currentUserId);
            document.getElementById('commentCount').textContent = data.commentCount;
            
            // Load version history ONLY if admin
            if (typeof isAdminUser !== 'undefined' && isAdminUser) {
                loadVersionHistory(documentId);
            }
            
            // Reset edit mode when opening modal
            if (typeof isAdminUser !== 'undefined' && isAdminUser) {
                cancelEditMode();
            }
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
    
    // Reset edit mode if active and user is admin
    if (typeof isAdminUser !== 'undefined' && isAdminUser && isEditMode) {
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
 * Archive document - ADMIN ONLY
 */
function archiveDocument() {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        Swal.fire({
            icon: 'error',
            title: 'Access Denied',
            text: 'Only administrators can archive documents',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    if (!currentDocumentId) return;
    
    Swal.fire({
        title: 'Archive this document?',
        html: `
            <p>This document will be moved to the archive and will no longer be visible in active documents.</p>
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
// COMMENT FUNCTIONS (Available to all users)
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
 * Add new comment (Available to all users)
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
 * Delete comment (Users can delete their own comments)
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
// DOCUMENT EDIT FUNCTIONS (ADMIN ONLY)
// ============================================================================

/**
 * Toggle edit mode - ADMIN ONLY
 */
function toggleEditMode() {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        return; // Silently fail - button shouldn't be visible anyway
    }
    
    isEditMode = true;
    
    // Store original values for cancellation
    originalDocumentData = {
        classificationId: document.getElementById('editClassification') ? document.getElementById('editClassification').value : '',
        tags: document.getElementById('editTags') ? document.getElementById('editTags').value : '',
        viewerGroupId: document.getElementById('editViewerGroup') ? document.getElementById('editViewerGroup').value : '',
        customGroupId: document.getElementById('editCustomGroup') ? document.getElementById('editCustomGroup').value : '',
        description: document.getElementById('editDescription') ? document.getElementById('editDescription').value : ''
    };
    
    // Show edit controls, hide view controls
    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'block');
    
    // Toggle buttons
    if (document.getElementById('btnEditMode')) {
        document.getElementById('btnEditMode').style.display = 'none';
    }
    if (document.getElementById('btnSaveChanges')) {
        document.getElementById('btnSaveChanges').style.display = 'inline-block';
    }
    if (document.getElementById('btnCancelEdit')) {
        document.getElementById('btnCancelEdit').style.display = 'inline-block';
    }
    
    // Show custom group container if currently set to custom group
    if (document.getElementById('editViewerGroup')) {
        const viewerGroupId = parseInt(document.getElementById('editViewerGroup').value);
        if (viewerGroupId === 5 && document.getElementById('editCustomGroupContainer')) {
            document.getElementById('editCustomGroupContainer').style.display = 'block';
        }
    }
}

/**
 * Cancel edit mode - ADMIN ONLY
 */
function cancelEditMode() {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        return;
    }
    
    isEditMode = false;
    
    // Restore original values
    if (document.getElementById('editClassification')) {
        document.getElementById('editClassification').value = originalDocumentData.classificationId || '';
    }
    if (document.getElementById('editTags')) {
        document.getElementById('editTags').value = originalDocumentData.tags || '';
    }
    if (document.getElementById('editViewerGroup')) {
        document.getElementById('editViewerGroup').value = originalDocumentData.viewerGroupId || '1';
    }
    if (document.getElementById('editCustomGroup')) {
        document.getElementById('editCustomGroup').value = originalDocumentData.customGroupId || '';
    }
    if (document.getElementById('editDescription')) {
        document.getElementById('editDescription').value = originalDocumentData.description || '';
    }
    
    // Show view controls, hide edit controls
    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'block');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'none');
    
    // Toggle buttons
    if (document.getElementById('btnEditMode')) {
        document.getElementById('btnEditMode').style.display = 'inline-block';
    }
    if (document.getElementById('btnSaveChanges')) {
        document.getElementById('btnSaveChanges').style.display = 'none';
    }
    if (document.getElementById('btnCancelEdit')) {
        document.getElementById('btnCancelEdit').style.display = 'none';
    }
    
    // Hide custom group container
    if (document.getElementById('editCustomGroupContainer')) {
        document.getElementById('editCustomGroupContainer').style.display = 'none';
    }
}

/**
 * Toggle custom group visibility - ADMIN ONLY
 */
function toggleCustomGroupEdit() {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        return;
    }
    
    if (!document.getElementById('editViewerGroup')) return;
    
    const viewerGroupId = parseInt(document.getElementById('editViewerGroup').value);
    const customGroupContainer = document.getElementById('editCustomGroupContainer');
    
    if (!customGroupContainer) return;
    
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
 * Save document changes - ADMIN ONLY
 */
function saveDocumentChanges() {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        Swal.fire({
            icon: 'error',
            title: 'Access Denied',
            text: 'Only administrators can edit documents',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    const classificationId = document.getElementById('editClassification') ? document.getElementById('editClassification').value : '';
    const tags = document.getElementById('editTags') ? document.getElementById('editTags').value : '';
    const viewerGroupId = document.getElementById('editViewerGroup') ? document.getElementById('editViewerGroup').value : '';
    const customGroupId = document.getElementById('editCustomGroup') ? document.getElementById('editCustomGroup').value : '';
    const description = document.getElementById('editDescription') ? document.getElementById('editDescription').value : '';
    
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
 * Perform document update - ADMIN ONLY
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
 * Populate edit fields when document modal opens - ADMIN ONLY
 * This stores the data AND fills the hidden edit form fields
 */
function populateEditFields(data) {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        return;
    }
    
    // Convert tagNames array to comma-separated string
    const tagsString = data.tagNames && data.tagNames.length > 0 
        ? data.tagNames.join(', ') 
        : '';
    
    // STORE the original data for cancellation
    originalDocumentData = {
        classificationId: data.classificationId || '',
        tags: tagsString,
        viewerGroupId: data.viewerGroupId || '1',
        customGroupId: data.customGroupId || '',
        description: data.description || ''
    };
    
    // Populate the HIDDEN edit form fields
    if (document.getElementById('editClassification')) {
        document.getElementById('editClassification').value = originalDocumentData.classificationId;
    }
    
    if (document.getElementById('editTags')) {
        document.getElementById('editTags').value = originalDocumentData.tags;
    }
    
    if (document.getElementById('editViewerGroup')) {
        document.getElementById('editViewerGroup').value = originalDocumentData.viewerGroupId;
    }
    
    if (document.getElementById('editCustomGroup')) {
        document.getElementById('editCustomGroup').value = originalDocumentData.customGroupId;
    }
    
    if (document.getElementById('editDescription')) {
        document.getElementById('editDescription').value = originalDocumentData.description;
    }
    
    // Show/hide custom group container if viewer group is "Custom Group" (ID = 5)
    if (originalDocumentData.viewerGroupId == 5 && document.getElementById('editCustomGroupContainer')) {
        document.getElementById('editCustomGroupContainer').style.display = 'block';
    } else if (document.getElementById('editCustomGroupContainer')) {
        document.getElementById('editCustomGroupContainer').style.display = 'none';
    }
}

// ============================================================================
// VERSION HISTORY FUNCTIONS (ADMIN ONLY)
// ============================================================================

/**
 * Load version history - ADMIN ONLY
 */
function loadVersionHistory(documentId) {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        return;
    }
    
    if (!document.getElementById('versionHistoryList')) return;
    
    fetch(`/documents/${documentId}/versions`)
        .then(res => res.json())
        .then(versions => {
            const list = document.getElementById('versionHistoryList');
            if (document.getElementById('versionCount')) {
                document.getElementById('versionCount').textContent = versions.length;
            }

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
 * Open the upload version modal - ADMIN ONLY
 */
function openUploadVersionModal() {
    // Check if user is admin
    if (typeof isAdminUser === 'undefined' || !isAdminUser) {
        Swal.fire({
            icon: 'error',
            title: 'Access Denied',
            text: 'Only administrators can upload new versions',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    if (!currentDocumentId) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No document selected',
            confirmButtonColor: '#E8392F'
        });
        return;
    }
    
    if (!document.getElementById('uploadVersionModal')) {
        return; // Modal not present (shouldn't happen for admins)
    }
    
    document.getElementById('versionDocumentId').value = currentDocumentId;
    document.getElementById('uploadVersionModal').style.display = 'block';
}

/**
 * Close the upload version modal
 */
function closeUploadVersionModal() {
    if (!document.getElementById('uploadVersionModal')) return;
    
    document.getElementById('uploadVersionModal').style.display = 'none';
    if (document.getElementById('uploadVersionForm')) {
        document.getElementById('uploadVersionForm').reset();
    }
}

/**
 * Confirm and upload new version - ADMIN ONLY
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
 * Perform the version upload - ADMIN ONLY
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
 * Note: Variables are passed from Thymeleaf inline script in HTML
 */
function confirmExport() {
    // Build export URL with all parameters
    let exportUrl = '/shared/export?';
    const params = [];

    // Add search query
    if (query && query.trim() !== '') {
        params.push(`query=${encodeURIComponent(query)}`);
    }

    // Add classification filter
    if (classificationId) {
        params.push(`classificationId=${classificationId}`);
    }

    // Add uploaded by filter
    if (uploadedById) {
        params.push(`uploadedById=${uploadedById}`);
    }

    // Add date filters
    if (startDate && startDate.trim() !== '') {
        params.push(`startDate=${encodeURIComponent(startDate)}`);
    }

    if (endDate && endDate.trim() !== '') {
        params.push(`endDate=${encodeURIComponent(endDate)}`);
    }

    // Add ADMIN-SPECIFIC FILTERS (only if admin)
    if (isAdminUser) {
        if (filterAreaId) {
            params.push(`filterAreaId=${filterAreaId}`);
        }

        if (filterOfficeId) {
            params.push(`filterOfficeId=${filterOfficeId}`);
        }

        if (filterDepartmentId) {
            params.push(`filterDepartmentId=${filterDepartmentId}`);
        }

        if (customGroupId) {
            params.push(`customGroupId=${customGroupId}`);
        }
    }

    // Build final URL
    if (params.length > 0) {
        exportUrl += params.join('&');
    }

    // Show confirmation dialog
    Swal.fire({
        title: 'Export Search Results',
        html: `
            <p>You are about to export <strong>${totalItems}</strong> documents.</p>
            ${getActiveFiltersDisplay()}
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#E8392F',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="fas fa-file-excel"></i> Export to Excel',
        cancelButtonText: 'Cancel'
    }).then((result) => {
        if (result.isConfirmed) {
            // Trigger download
            window.location.href = exportUrl;
            
            // Show success message
            Swal.fire({
                title: 'Exporting...',
                text: 'Your file is being prepared for download.',
                icon: 'success',
                timer: 2000,
                showConfirmButton: false
            });
        }
    });
}

/**
 * Generates HTML display of active filters for the confirmation dialog
 */
function getActiveFiltersDisplay() {
    const activeFilters = [];

    if (query && query.trim() !== '') {
        activeFilters.push(`Search: "${query}"`);
    }

    if (classificationId) {
        activeFilters.push('Classification filter applied');
    }

    if (uploadedById) {
        activeFilters.push('Uploaded by filter applied');
    }

    if (startDate || endDate) {
        if (startDate && endDate) {
            activeFilters.push(`Date range: ${startDate} to ${endDate}`);
        } else if (startDate) {
            activeFilters.push(`From: ${startDate}`);
        } else {
            activeFilters.push(`Until: ${endDate}`);
        }
    }

    // Add admin filter descriptions
    if (isAdminUser) {
        if (filterAreaId) {
            activeFilters.push('Area filter applied');
        }
        if (filterOfficeId) {
            activeFilters.push('Office filter applied');
        }
        if (filterDepartmentId) {
            activeFilters.push('Department filter applied');
        }
        if (customGroupId) {
            activeFilters.push('Custom Group filter applied');
        }
    }

    if (activeFilters.length === 0) {
        return '<p class="text-muted">No filters applied - exporting all documents.</p>';
    }

    return `
        <div class="text-start mt-3">
            <strong>Active Filters:</strong>
            <ul class="text-muted small mt-2">
                ${activeFilters.map(filter => `<li>${filter}</li>`).join('')}
            </ul>
        </div>
    `;
}

/**
 * Perform the Excel export
 * Uses variables defined in inline Thymeleaf script
 */
async function performExport() {
    const _totalItems = typeof totalItems !== 'undefined' ? totalItems : 0;
    const _query = typeof query !== 'undefined' ? query : '';
    const _classificationId = typeof classificationId !== 'undefined' ? classificationId : null;
    const _uploadedById = typeof uploadedById !== 'undefined' ? uploadedById : null;
    const _startDate = typeof startDate !== 'undefined' ? startDate : '';
    const _endDate = typeof endDate !== 'undefined' ? endDate : '';
    const _filterAreaId = typeof filterAreaId !== 'undefined' ? filterAreaId : null;
    const _filterOfficeId = typeof filterOfficeId !== 'undefined' ? filterOfficeId : null;
    const _filterDepartmentId = typeof filterDepartmentId !== 'undefined' ? filterDepartmentId : null;
    const _customGroupId = typeof customGroupId !== 'undefined' ? customGroupId : null;

    Swal.fire({
        title: 'Generating Excel...',
        html: `
            <p>Preparing ${_totalItems} records for export...</p>
            <div class="progress mt-3">
                <div class="progress-bar progress-bar-striped progress-bar-animated"
                     style="width: 100%"></div>
            </div>
            <p class="text-muted mt-2">Please wait…</p>
        `,
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => Swal.showLoading()
    });

    const params = new URLSearchParams();
    if (_query) params.append('query', _query);
    if (_classificationId) params.append('classificationId', _classificationId);
    if (_uploadedById) params.append('uploadedById', _uploadedById);
    if (_startDate) params.append('startDate', _startDate);
    if (_endDate) params.append('endDate', _endDate);
    if (_filterAreaId) params.append('filterAreaId', _filterAreaId);
    if (_filterOfficeId) params.append('filterOfficeId', _filterOfficeId);
    if (_filterDepartmentId) params.append('filterDepartmentId', _filterDepartmentId);
    if (_customGroupId) params.append('customGroupId', _customGroupId);

    const exportUrl = `/shared/export?${params.toString()}`;

    try {
        const response = await fetch(exportUrl, { method: 'GET' });

        if (!response.ok) {
            throw new Error('Export failed');
        }

        const blob = await response.blob();

        // Get filename from response header
        let filename = 'shared-files.xlsx';
        const disposition = response.headers.get('Content-Disposition');
        if (disposition && disposition.includes('filename=')) {
            filename = disposition.split('filename=')[1].replace(/"/g, '');
        }

        // Trigger download
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();

        Swal.fire({
            icon: 'success',
            title: 'Export complete',
            text: 'Your Excel file has been downloaded.',
            timer: 2000,
            showConfirmButton: false
        });

    } catch (err) {
        console.error(err);
        Swal.fire({
            icon: 'error',
            title: 'Export failed',
            text: 'Unable to generate the Excel file.'
        });
    }
}


/**
 * Build filter summary HTML
 */
function buildFilterSummary(query, classificationId, uploadedById, startDate, endDate) {
    const filters = [];
    
    if (query) {
        filters.push(`<li><strong>Search:</strong> "${escapeHtml(query)}"</li>`);
    }
    
    if (classificationId) {
        const classSelect = document.querySelector('[name="classificationId"]');
        const classText = classSelect ? classSelect.options[classSelect.selectedIndex].text : 'Selected';
        filters.push(`<li><strong>Classification:</strong> ${escapeHtml(classText)}</li>`);
    }
    
    if (uploadedById) {
        const userSelect = document.querySelector('[name="uploadedById"]');
        const userText = userSelect ? userSelect.options[userSelect.selectedIndex].text : 'Selected';
        filters.push(`<li><strong>Uploaded By:</strong> ${escapeHtml(userText)}</li>`);
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
    
    // Version upload form handler (ADMIN ONLY)
    const uploadForm = document.getElementById('uploadVersionForm');
    if (uploadForm && typeof isAdminUser !== 'undefined' && isAdminUser) {
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
    
    // File input preview (ADMIN ONLY)
    const fileInput = document.getElementById('versionFile');
    if (fileInput && typeof isAdminUser !== 'undefined' && isAdminUser) {
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
    const docIdToOpen = sessionStorage.getItem('openDocumentModal');
    
    if (docIdToOpen) {
        // Clear the sessionStorage
        sessionStorage.removeItem('openDocumentModal');
        
        // Small delay to ensure page is fully loaded
        setTimeout(function() {
            // Trigger the view button click for this document
            const viewButton = document.querySelector(`button[data-id="${docIdToOpen}"]`);
            if (viewButton) {
                viewButton.click();
            } else {
                // If button not found on current page, try opening directly
                console.log('Document not found on current page, opening modal directly...');
                openDocumentModal(parseInt(docIdToOpen));
            }
        }, 300);
    }
});

// Close modals when clicking outside
window.onclick = function(event) {
    const documentModal = document.getElementById('documentViewerModal');
    const uploadModal = document.getElementById('uploadVersionModal');
    
    if (event.target === documentModal) {
        closeDocumentModal();
    }
    if (uploadModal && event.target === uploadModal) {
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