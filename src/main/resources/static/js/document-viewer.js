let currentDocumentId = null;

/**
 * Open document viewer modal
 */
function openDocumentModal(documentId) {
    currentDocumentId = documentId;
    
    // Show modal
    document.getElementById('documentViewerModal').style.display = 'block';
    
    // Load document details
    fetch(`/documents/details/${documentId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load document');
            }
            return response.json();
        })
        .then(data => {
            // Populate document info
            document.getElementById('modalDocTitle').textContent = data.title;
            document.getElementById('infoFilename').textContent = data.filename;
            document.getElementById('infoUploadedBy').textContent = data.uploadedBy;
            document.getElementById('infoUploadDate').textContent = formatDate(data.dateCreated);
            document.getElementById('infoClassification').textContent = data.documentClassification;
            document.getElementById('infoViewerGroup').textContent = data.intendedViewerGroup;
            document.getElementById('infoDescription').textContent = data.description || 'No description';
            
            // Load document in iframe
            document.getElementById('documentFrame').src = `/documents/view/${documentId}`;
            
            // Load comments
            loadComments(data.comments);
            document.getElementById('commentCount').textContent = data.commentCount;
        })
        .catch(error => {
            console.error('Error loading document:', error);
            alert('Failed to load document details');
            closeDocumentModal();
        });
}

/**
 * Close modal
 */
function closeDocumentModal() {
    document.getElementById('documentViewerModal').style.display = 'none';
    document.getElementById('documentFrame').src = '';
    currentDocumentId = null;
}

/**
 * Load comments into the comments section
 */
function loadComments(comments) {
    const commentsList = document.getElementById('commentsList');
    commentsList.innerHTML = '';
    
    if (comments.length === 0) {
        commentsList.innerHTML = '<p style="color: #666;">No comments yet. Be the first to comment!</p>';
        return;
    }
    
    comments.forEach(comment => {
        const commentDiv = document.createElement('div');
        commentDiv.className = 'comment-item';
        commentDiv.innerHTML = `
            <div class="comment-author">
                ${escapeHtml(comment.user.username)}
                <span class="comment-date">${formatDate(comment.createdDate)}</span>
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
        alert('Please enter a comment');
        return;
    }
    
    const formData = new FormData();
    formData.append('commentText', commentText);
    
    fetch(`/documents/${currentDocumentId}/comments`, {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to add comment');
        }
        return response.json();
    })
    .then(comment => {
        // Clear textarea
        document.getElementById('commentText').value = '';
        
        // Reload document details to refresh comments
        openDocumentModal(currentDocumentId);
    })
    .catch(error => {
        console.error('Error adding comment:', error);
        alert('Failed to add comment');
    });
}

/**
 * Download document
 */
function downloadDocument() {
    window.open(`/documents/download/${currentDocumentId}`, '_blank');
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
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('documentViewerModal');
    if (event.target == modal) {
        closeDocumentModal();
    }
}

// Close modal on ESC key
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeDocumentModal();
    }
});