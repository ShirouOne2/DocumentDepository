/**
 * My Files Modal - Document Viewer Modal Functionality
 * Handles opening/closing modal and loading document details
 */

let currentDocumentId = null;
let currentUserIdGlobal = null;

/**
 * Open document viewer modal and load document details
 */
function openDocumentModal(documentId) {
    currentDocumentId = documentId;
    document.getElementById('documentViewerModal').style.display = 'block';
    
    // Fetch document details
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
            
            // Populate edit fields (if document-edit.js is loaded)
            if (typeof populateEditFields === 'function') {
                populateEditFields(data);
            }
            
            // Load comments
            loadComments(data.comments, data.currentUserId);
            document.getElementById('commentCount').textContent = data.commentCount;
            
            // Load version history
            loadVersionHistory(documentId);
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
    if (typeof cancelEditMode === 'function' && isEditMode) {
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
        // Reload the modal to show new comment
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

// Event Listeners
document.addEventListener('DOMContentLoaded', function() {
    // Attach click handlers to view buttons
    document.querySelectorAll('.btn-view').forEach(btn => {
        btn.addEventListener('click', function () {
            const id = this.dataset.id;
            openDocumentModal(id);
        });
    });
});

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('documentViewerModal');
    if (event.target == modal) {
        closeDocumentModal();
    }
}

// Close modal with Escape key
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        const modal = document.getElementById('documentViewerModal');
        const uploadModal = document.getElementById('uploadVersionModal');
        
        if (modal && modal.style.display === 'block') {
            closeDocumentModal();
        }
        if (uploadModal && uploadModal.style.display === 'block') {
            closeUploadVersionModal();
        }
    }
});