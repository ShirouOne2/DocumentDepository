/**
 * DevTools Department Management Module
 */

const DevToolsDepartments = {
    
    init() {
        document.querySelectorAll('.btn-edit-department').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.edit(e.currentTarget.dataset.departmentId, e.currentTarget.dataset.departmentName);
            });
        });

        document.querySelectorAll('.btn-delete-department').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.delete(e.currentTarget.dataset.departmentId, e.currentTarget.dataset.departmentName);
            });
        });
    },

    openAddModal() {
        document.getElementById('departmentModalTitle').innerHTML = '<i class="fas fa-building me-2"></i>Add Department';
        document.getElementById('departmentForm').action = '/devtools/departments/add';
        document.getElementById('departmentId').value = '';
        document.getElementById('departmentName').value = '';
        DevToolsUtils.openModal('departmentModal');
    },

    edit(id, name) {
        document.getElementById('departmentModalTitle').innerHTML = '<i class="fas fa-building me-2"></i>Edit Department';
        document.getElementById('departmentForm').action = '/devtools/departments/update';
        document.getElementById('departmentId').value = id;
        document.getElementById('departmentName').value = name;
        DevToolsUtils.openModal('departmentModal');
    },

    closeModal() {
        DevToolsUtils.closeModal('departmentModal');
    },

    submit() {
        const name = document.getElementById('departmentName').value.trim();
        const isEdit = document.getElementById('departmentId').value !== '';
        
        if (!name) {
            DevToolsUtils.showValidationError('Please enter a department name');
            return;
        }
        
        DevToolsUtils.confirmSave(isEdit, 'Department', () => {
            document.getElementById('departmentForm').submit();
        });
    },

    delete(id, name) {
        DevToolsUtils.confirmDelete('Department', name, () => {
            DevToolsUtils.submitDeleteForm('/devtools/departments/delete', 'departmentId', id);
        });
    }
};

// Global functions for HTML onclick handlers
function openAddDepartmentModal() { DevToolsDepartments.openAddModal(); }
function closeDepartmentModal() { DevToolsDepartments.closeModal(); }
function submitDepartmentForm() { DevToolsDepartments.submit(); }