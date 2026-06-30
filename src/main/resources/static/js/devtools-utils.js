/**
 * DevTools Utility Functions
 * Shared helper functions used across all modules
 */

const DevToolsUtils = {
    
    /**
     * Submit a delete form dynamically
     */
    submitDeleteForm(action, fieldName, id) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = action;
        
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = fieldName;
        input.value = id;
        
        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    },

    /**
     * Show confirmation dialog for add/edit operations
     */
    confirmSave(isEdit, itemName, callback) {
        Swal.fire({
            title: isEdit ? `Update ${itemName}?` : `Add ${itemName}?`,
            text: `Are you sure you want to ${isEdit ? 'update' : 'add'} this ${itemName.toLowerCase()}?`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#E8392F',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, save it!',
            cancelButtonText: 'Cancel'
        }).then((result) => {
            if (result.isConfirmed) {
                callback();
            }
        });
    },

    /**
     * Show confirmation dialog for delete operations
     */
    confirmDelete(itemType, itemName, callback) {
        Swal.fire({
            title: `Delete ${itemType}?`,
            html: `Are you sure you want to delete "<strong>${itemName}</strong>"?<br><small class="text-danger">This action cannot be undone!</small>`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, delete it!',
            cancelButtonText: 'Cancel'
        }).then((result) => {
            if (result.isConfirmed) {
                callback();
            }
        });
    },

    /**
     * Show validation error
     */
    showValidationError(message) {
        Swal.fire({
            icon: 'warning',
            title: 'Missing Information',
            text: message,
            confirmButtonColor: '#E8392F'
        });
    },

    /**
     * Show restriction warning
     */
    showRestrictionWarning(message) {
        Swal.fire({
            icon: 'warning',
            title: 'Restricted',
            text: message,
            confirmButtonColor: '#E8392F'
        });
    },

    /**
     * Validate required fields
     */
    validateRequired(fields) {
        for (let field of fields) {
            if (!field.value || !field.value.trim()) {
                return false;
            }
        }
        return true;
    },

    /**
     * Open modal helper
     */
    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'block';
        }
    },

    /**
     * Close modal helper
     */
    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    }
};