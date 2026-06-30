/**
 * Document Edit Functionality
 * Handles editing of classification, tags, viewer groups, and description
 * Works for both "My Files" (owner) and "Shared Files" (admin)
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

function cancelEditMode() {
    isEditMode = false;
    
    // Restore original values
    document.getElementById('editClassification').value = originalDocumentData.classificationId;
    document.getElementById('editTags').value = originalDocumentData.tags;
    document.getElementById('editViewerGroup').value = originalDocumentData.viewerGroupId;
    if (document.getElementById('editCustomGroup')) {
        document.getElementById('editCustomGroup').value = originalDocumentData.customGroupId;
    }
    document.getElementById('editDescription').value = originalDocumentData.description;
    
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
            // Reload the page to show updated data
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
    
    // Reset edit mode when opening modal
    if (typeof cancelEditMode === 'function') {
        cancelEditMode();
    }
}