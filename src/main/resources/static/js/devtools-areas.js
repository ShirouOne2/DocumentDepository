/**
 * DevTools Area Management Module
 */

const DevToolsAreas = {
    
    init() {
        document.querySelectorAll('.btn-edit-area').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.edit(e.currentTarget.dataset.areaId, e.currentTarget.dataset.areaName);
            });
        });

        document.querySelectorAll('.btn-delete-area').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.delete(e.currentTarget.dataset.areaId, e.currentTarget.dataset.areaName);
            });
        });
    },

    openAddModal() {
        document.getElementById('areaModalTitle').innerHTML = '<i class="fas fa-map-marked-alt me-2"></i>Add Area';
        document.getElementById('areaForm').action = '/devtools/areas/add';
        document.getElementById('areaId').value = '';
        document.getElementById('areaName').value = '';
        DevToolsUtils.openModal('areaModal');
    },

    edit(id, name) {
        document.getElementById('areaModalTitle').innerHTML = '<i class="fas fa-map-marked-alt me-2"></i>Edit Area';
        document.getElementById('areaForm').action = '/devtools/areas/update';
        document.getElementById('areaId').value = id;
        document.getElementById('areaName').value = name;
        DevToolsUtils.openModal('areaModal');
    },

    closeModal() {
        DevToolsUtils.closeModal('areaModal');
    },

    submit() {
        const name = document.getElementById('areaName').value.trim();
        const isEdit = document.getElementById('areaId').value !== '';
        
        if (!name) {
            DevToolsUtils.showValidationError('Please enter an area name');
            return;
        }
        
        DevToolsUtils.confirmSave(isEdit, 'Area', () => {
            document.getElementById('areaForm').submit();
        });
    },

    delete(id, name) {
        DevToolsUtils.confirmDelete('Area', name, () => {
            DevToolsUtils.submitDeleteForm('/devtools/areas/delete', 'areaId', id);
        });
    }
};

// Global functions for HTML onclick handlers
function openAddAreaModal() { DevToolsAreas.openAddModal(); }
function closeAreaModal() { DevToolsAreas.closeModal(); }
function submitAreaForm() { DevToolsAreas.submit(); }