/**
 * DevTools Position Management Module
 */

const DevToolsPositions = {
    
    init() {
        document.querySelectorAll('.btn-edit-position').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.edit(e.currentTarget.dataset.positionId, e.currentTarget.dataset.positionName);
            });
        });

        document.querySelectorAll('.btn-delete-position').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.delete(e.currentTarget.dataset.positionId, e.currentTarget.dataset.positionName);
            });
        });
    },

    openAddModal() {
        document.getElementById('positionModalTitle').innerHTML = '<i class="fas fa-briefcase me-2"></i>Add Position';
        document.getElementById('positionForm').action = '/devtools/positions/add';
        document.getElementById('positionId').value = '';
        document.getElementById('positionName').value = '';
        DevToolsUtils.openModal('positionModal');
    },

    edit(id, name) {
        document.getElementById('positionModalTitle').innerHTML = '<i class="fas fa-briefcase me-2"></i>Edit Position';
        document.getElementById('positionForm').action = '/devtools/positions/update';
        document.getElementById('positionId').value = id;
        document.getElementById('positionName').value = name;
        DevToolsUtils.openModal('positionModal');
    },

    closeModal() {
        DevToolsUtils.closeModal('positionModal');
    },

    submit() {
        const name = document.getElementById('positionName').value.trim();
        const isEdit = document.getElementById('positionId').value !== '';
        
        if (!name) {
            DevToolsUtils.showValidationError('Please enter a position name');
            return;
        }
        
        DevToolsUtils.confirmSave(isEdit, 'Position', () => {
            document.getElementById('positionForm').submit();
        });
    },

    delete(id, name) {
        DevToolsUtils.confirmDelete('Position', name, () => {
            DevToolsUtils.submitDeleteForm('/devtools/positions/delete', 'positionId', id);
        });
    }
};

// Global functions for HTML onclick handlers
function openAddPositionModal() { DevToolsPositions.openAddModal(); }
function closePositionModal() { DevToolsPositions.closeModal(); }
function submitPositionForm() { DevToolsPositions.submit(); }