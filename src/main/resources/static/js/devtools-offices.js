/**
 * DevTools Office Management Module
 */

const DevToolsOffices = {
    
    init() {
        document.querySelectorAll('.btn-edit-office').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.edit(e.currentTarget.dataset);
            });
        });

        document.querySelectorAll('.btn-delete-office').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.delete(e.currentTarget.dataset.officeId, e.currentTarget.dataset.officeName);
            });
        });
    },

    openAddModal() {
        document.getElementById('officeModalTitle').innerHTML = '<i class="fas fa-door-open me-2"></i>Add Office';
        document.getElementById('officeForm').action = '/devtools/offices/add';
        document.getElementById('officeId').value = '';
        document.getElementById('officeName').value = '';
        // document.getElementById('officeDepartment').value = '';
        document.getElementById('officeArea').value = '';
        DevToolsUtils.openModal('officeModal');
    },

    edit(data) {
        document.getElementById('officeModalTitle').innerHTML = '<i class="fas fa-door-open me-2"></i>Edit Office';
        document.getElementById('officeForm').action = '/devtools/offices/update';
        document.getElementById('officeId').value = data.officeId;
        document.getElementById('officeName').value = data.officeName;
        // document.getElementById('officeDepartment').value = data.departmentId;
        document.getElementById('officeArea').value = data.areaId;
        DevToolsUtils.openModal('officeModal');
    },

    closeModal() {
        DevToolsUtils.closeModal('officeModal');
    },

    submit() {
        const name = document.getElementById('officeName').value.trim();
        // const departmentId = document.getElementById('officeDepartment').value;
        const areaId = document.getElementById('officeArea').value;
        const isEdit = document.getElementById('officeId').value !== '';
        
        if (!name || !areaId) {
            DevToolsUtils.showValidationError('Please fill in all fields');
            return;
        }
        
        DevToolsUtils.confirmSave(isEdit, 'Office', () => {
            document.getElementById('officeForm').submit();
        });
    },

    delete(id, name) {
        DevToolsUtils.confirmDelete('Office', name, () => {
            DevToolsUtils.submitDeleteForm('/devtools/offices/delete', 'officeId', id);
        });
    }
};

// Global functions for HTML onclick handlers
function openAddOfficeModal() { DevToolsOffices.openAddModal(); }
function closeOfficeModal() { DevToolsOffices.closeModal(); }
function submitOfficeForm() { DevToolsOffices.submit(); }