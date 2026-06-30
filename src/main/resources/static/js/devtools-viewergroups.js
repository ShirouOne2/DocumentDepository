/**
 * DevTools Viewer Group Management Module
 */

const DevToolsViewerGroups = {
    
    init() {
        document.querySelectorAll('.btn-edit-viewergroup').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.edit(e.currentTarget.dataset.groupId, e.currentTarget.dataset.groupName);
            });
        });

        document.querySelectorAll('.btn-delete-viewergroup').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.delete(e.currentTarget.dataset.groupId, e.currentTarget.dataset.groupName);
            });
        });
    },

    openAddModal() {
        document.getElementById('viewerGroupModalTitle').innerHTML = '<i class="fas fa-users me-2"></i>Add Viewer Group';
        document.getElementById('viewerGroupForm').action = '/devtools/viewergroups/add';
        document.getElementById('viewerGroupId').value = '';
        document.getElementById('viewerGroupName').value = '';
        DevToolsUtils.openModal('viewerGroupModal');
    },

    edit(id, name) {
        document.getElementById('viewerGroupModalTitle').innerHTML = '<i class="fas fa-users me-2"></i>Edit Viewer Group';
        document.getElementById('viewerGroupForm').action = '/devtools/viewergroups/update';
        document.getElementById('viewerGroupId').value = id;
        document.getElementById('viewerGroupName').value = name;
        DevToolsUtils.openModal('viewerGroupModal');
    },

    closeModal() {
        DevToolsUtils.closeModal('viewerGroupModal');
    },

    submit() {
        const name = document.getElementById('viewerGroupName').value.trim();
        const isEdit = document.getElementById('viewerGroupId').value !== '';
        
        if (!name) {
            DevToolsUtils.showValidationError('Please enter a viewer group name');
            return;
        }
        
        DevToolsUtils.confirmSave(isEdit, 'Viewer Group', () => {
            document.getElementById('viewerGroupForm').submit();
        });
    },

    delete(id, name) {
        DevToolsUtils.confirmDelete('Viewer Group', name, () => {
            DevToolsUtils.submitDeleteForm('/devtools/viewergroups/delete', 'groupId', id);
        });
    }
};

// Global functions for HTML onclick handlers
function openAddViewerGroupModal() { DevToolsViewerGroups.openAddModal(); }
function closeViewerGroupModal() { DevToolsViewerGroups.closeModal(); }
function submitViewerGroupForm() { DevToolsViewerGroups.submit(); }