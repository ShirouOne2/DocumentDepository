/**
 * Version Upload Functionality
 * Handles uploading new versions of documents
 */

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
 * Handle version upload form submission
 */
document.addEventListener('DOMContentLoaded', function() {
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
            
            // Validate file size (optional - adjust as needed)
            const maxSize = 50 * 1024 * 1024; // 50MB
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
});

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
 * Preview selected file details
 */
document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('versionFile');
    
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                console.log('Selected file:', file.name, formatFileSize(file.size));
            }
        });
    }
});

// Close upload modal when clicking outside
window.addEventListener('click', function(event) {
    const modal = document.getElementById('uploadVersionModal');
    if (event.target === modal) {
        closeUploadVersionModal();
    }
});