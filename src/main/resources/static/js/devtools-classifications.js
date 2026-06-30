/**
 * DevTools Classification Management Module
 */

const DevToolsClassifications = {
    
    init() {
        document.querySelectorAll('.btn-edit-classification').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.edit(e.currentTarget.dataset.classificationId, e.currentTarget.dataset.classificationName);
            });
        });

        document.querySelectorAll('.btn-delete-classification').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.delete(e.currentTarget.dataset.classificationId, e.currentTarget.dataset.classificationName);
            });
        });
    },

    openAddModal() {
        document.getElementById('classificationModalTitle').innerHTML = '<i class="fas fa-file-alt me-2"></i>Add Classification';
        document.getElementById('classificationForm').action = '/devtools/classifications/add';
        document.getElementById('classificationId').value = '';
        document.getElementById('classificationName').value = '';
        DevToolsUtils.openModal('classificationModal');
    },

    edit(id, name) {
        document.getElementById('classificationModalTitle').innerHTML = '<i class="fas fa-file-alt me-2"></i>Edit Classification';
        document.getElementById('classificationForm').action = '/devtools/classifications/update';
        document.getElementById('classificationId').value = id;
        document.getElementById('classificationName').value = name;
        DevToolsUtils.openModal('classificationModal');
    },

    closeModal() {
        DevToolsUtils.closeModal('classificationModal');
    },

    submit() {
        const name = document.getElementById('classificationName').value.trim();
        const isEdit = document.getElementById('classificationId').value !== '';
        
        if (!name) {
            DevToolsUtils.showValidationError('Please enter a classification name');
            return;
        }
        
        DevToolsUtils.confirmSave(isEdit, 'Classification', () => {
            document.getElementById('classificationForm').submit();
        });
    },

    delete(id, name) {
        DevToolsUtils.confirmDelete('Classification', name, () => {
            DevToolsUtils.submitDeleteForm('/devtools/classifications/delete', 'classificationId', id);
        });
    }
};

// Global functions for HTML onclick handlers
function openAddClassificationModal() { DevToolsClassifications.openAddModal(); }
function closeClassificationModal() { DevToolsClassifications.closeModal(); }
function submitClassificationForm() { DevToolsClassifications.submit(); }