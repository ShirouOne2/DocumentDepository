/**
 * DevTools User Management Module
 * Includes cascading office → department dropdown logic.
 */

const DevToolsUsers = {

    init() {
        this.initializeSearch();
        this.initializeEditButtons();
        this.initializeResetPasswordButtons();
        this.initializePasswordValidation();
        this.initializeOfficeCascade();
    },

    // ─────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────
    initializeSearch() {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keyup', function () {
                const searchValue = this.value.toLowerCase();
                document.querySelectorAll('#userTable tbody tr').forEach(row => {
                    row.style.display = row.textContent.toLowerCase().includes(searchValue) ? '' : 'none';
                });
            });
        }
    },

    // ─────────────────────────────────────────
    // CASCADING OFFICE → DEPARTMENT
    // ─────────────────────────────────────────
    initializeOfficeCascade() {
        const addOfficeSelect    = document.getElementById('addOffice');
        const editOfficeSelect   = document.getElementById('editOffice');

        if (addOfficeSelect) {
            addOfficeSelect.addEventListener('change', () => {
                this.loadDepartments(addOfficeSelect.value, 'addDepartment');
            });
        }

        if (editOfficeSelect) {
            editOfficeSelect.addEventListener('change', () => {
                this.loadDepartments(editOfficeSelect.value, 'editDepartment');
            });
        }
    },

    /**
     * Fetch departments for the given office and populate the select.
     * If the office has no departments, the select is disabled with a placeholder.
     *
     * @param {string|number} officeId      - Selected office ID
     * @param {string}        selectId      - ID of the <select> to populate
     * @param {string|number} [currentDeptId] - Pre-select this dept (used when opening edit modal)
     */
    loadDepartments(officeId, selectId, currentDeptId = null) {
        const deptSelect = document.getElementById(selectId);
        if (!deptSelect) return;

        if (!officeId) {
            deptSelect.innerHTML = '<option value="">Select office first</option>';
            deptSelect.disabled = true;
            return;
        }

        deptSelect.innerHTML = '<option value="">Loading...</option>';
        deptSelect.disabled = true;

        fetch(`/api/departments/by-office/${officeId}`)
            .then(r => r.json())
            .then(departments => {
                if (departments.length === 0) {
                    // Office has no departments — leave null, backend handles it
                    deptSelect.innerHTML = '<option value="">No departments for this office</option>';
                    deptSelect.disabled = true;
                } else {
                    deptSelect.innerHTML = '<option value="">Select Department</option>';
                    departments.forEach(d => {
                        const opt = document.createElement('option');
                        opt.value = d.id;
                        opt.textContent = d.name;
                        if (currentDeptId && String(d.id) === String(currentDeptId)) {
                            opt.selected = true;
                        }
                        deptSelect.appendChild(opt);
                    });
                    deptSelect.disabled = false;
                }
            })
            .catch(err => {
                console.error('Error loading departments:', err);
                deptSelect.innerHTML = '<option value="">Error loading departments</option>';
                deptSelect.disabled = true;
            });
    },

    // ─────────────────────────────────────────
    // EDIT / RESET PASSWORD BUTTON INIT
    // ─────────────────────────────────────────
    initializeEditButtons() {
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const isAdmin = e.currentTarget.dataset.isAdmin === 'true';
                if (isAdmin) {
                    DevToolsUtils.showRestrictionWarning('Admin accounts cannot be edited!');
                    return;
                }
                this.openEditModal(e.currentTarget.dataset);
            });
        });
    },

    initializeResetPasswordButtons() {
        document.querySelectorAll('.btn-reset-password').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const isAdmin = e.currentTarget.dataset.isAdmin === 'true';
                if (isAdmin) {
                    DevToolsUtils.showRestrictionWarning('Admin passwords cannot be reset!');
                    return;
                }
                this.openResetPasswordModal(e.currentTarget.dataset);
            });
        });
    },

    // ─────────────────────────────────────────
    // PASSWORD VALIDATION
    // ─────────────────────────────────────────
    initializePasswordValidation() {
        const addConfirmPassword = document.getElementById('addConfirmPassword');
        if (addConfirmPassword) {
            addConfirmPassword.addEventListener('input', () => this.validatePasswordMatch('add'));
        }

        const confirmPassword = document.getElementById('confirmPassword');
        if (confirmPassword) {
            confirmPassword.addEventListener('input', () => this.validatePasswordMatch('reset'));
        }
    },

    validatePasswordMatch(mode) {
        const newPassword     = mode === 'add'
            ? document.getElementById('addPassword')?.value
            : document.getElementById('newPassword')?.value;
        const confirmPassword = document.getElementById(mode === 'add' ? 'addConfirmPassword' : 'confirmPassword').value;
        const errorMsg        = document.getElementById(mode === 'add' ? 'addPasswordMatchError' : 'passwordMatchError');
        const submitBtn       = document.getElementById(mode === 'add' ? 'addUserBtn' : 'resetPasswordBtn');

        if (confirmPassword && newPassword !== confirmPassword) {
            errorMsg.style.display = 'block';
            submitBtn.disabled = true;
        } else {
            errorMsg.style.display = 'none';
            submitBtn.disabled = false;
        }
    },

    // ─────────────────────────────────────────
    // MODAL OPEN / CLOSE
    // ─────────────────────────────────────────
    openAddModal() {
        document.getElementById('addUserForm').reset();
        document.getElementById('addPasswordMatchError').style.display = 'none';
        document.getElementById('addUserBtn').disabled = false;

        // Reset department dropdown to disabled placeholder
        const deptSelect = document.getElementById('addDepartment');
        if (deptSelect) {
            deptSelect.innerHTML = '<option value="">Select office first</option>';
            deptSelect.disabled = true;
        }

        DevToolsUtils.openModal('addUserModal');
    },

    closeAddModal() {
        DevToolsUtils.closeModal('addUserModal');
    },

    openEditModal(data) {
        document.getElementById('editUserId').value   = data.userId;
        document.getElementById('editUsername').value = data.username;
        document.getElementById('editEmail').value    = data.email;
        document.getElementById('editPosition').value = data.positionId;
        document.getElementById('editOffice').value   = data.officeId;
        document.getElementById('editRole').value     = data.roleId;
        document.getElementById('editIsActive').value = data.isActive;

        // Load departments for the selected office, then pre-select the user's dept
        this.loadDepartments(data.officeId, 'editDepartment', data.departmentId);

        const isActive       = data.isActive === 'true';
        const archiveBtn     = document.getElementById('archiveUserBtn');
        const archiveBtnText = document.getElementById('archiveButtonText');

        if (isActive) {
            archiveBtn.className    = 'btn btn-danger';
            archiveBtnText.textContent = 'Archive User';
        } else {
            archiveBtn.className    = 'btn btn-success';
            archiveBtnText.textContent = 'Restore User';
        }

        DevToolsUtils.openModal('editUserModal');
    },

    closeEditModal() {
        DevToolsUtils.closeModal('editUserModal');
    },

    openResetPasswordModal(data) {
        document.getElementById('resetUserId').value              = data.userId;
        document.getElementById('resetUsername').textContent      = data.username;
        document.getElementById('newPassword').value              = '';
        document.getElementById('confirmPassword').value          = '';
        document.getElementById('passwordMatchError').style.display = 'none';
        document.getElementById('resetPasswordBtn').disabled      = false;
        DevToolsUtils.openModal('resetPasswordModal');
    },

    closeResetPasswordModal() {
        DevToolsUtils.closeModal('resetPasswordModal');
    },

    // ─────────────────────────────────────────
    // CONFIRM ACTIONS
    // ─────────────────────────────────────────
    confirmAddUser() {
        const username        = document.getElementById('addUsername').value.trim();
        const email           = document.getElementById('addEmail').value.trim();
        const password        = document.getElementById('addPassword').value;
        const confirmPassword = document.getElementById('addConfirmPassword').value;
        const positionId      = document.getElementById('addPosition').value;
        const officeId        = document.getElementById('addOffice').value;
        const roleId          = document.getElementById('addRole').value;
        // Department is optional — office may have none
        const deptSelect      = document.getElementById('addDepartment');
        const departmentId    = deptSelect && !deptSelect.disabled ? deptSelect.value : null;

        if (!username || !email || !password || !confirmPassword || !positionId || !officeId || !roleId) {
            DevToolsUtils.showValidationError('Please fill in all required fields');
            return;
        }

        if (password.length < 6) {
            DevToolsUtils.showValidationError('Password must be at least 6 characters');
            return;
        }

        if (password !== confirmPassword) {
            Swal.fire({
                icon: 'error',
                title: 'Passwords Do Not Match',
                text: 'Please make sure both passwords match',
                confirmButtonColor: '#E8392F'
            });
            return;
        }

        Swal.fire({
            title: 'Add New User?',
            html: `Are you sure you want to add user <strong>${username}</strong>?`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#E8392F',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, add user!',
            cancelButtonText: 'Cancel'
        }).then((result) => {
            if (result.isConfirmed) {
                document.getElementById('addUserBtn').disabled = true;
                document.getElementById('addUserForm').submit();
            }
        });
    },

    confirmEditUser() {
        DevToolsUtils.confirmSave(true, 'User', () => {
            document.getElementById('editUserForm').submit();
        });
    },

    confirmArchiveUser() {
        const userId   = document.getElementById('editUserId').value;
        const username = document.getElementById('editUsername').value;
        const isActive = document.getElementById('editIsActive').value === 'true';
        const action   = isActive ? 'archive' : 'restore';

        Swal.fire({
            title: `${action.charAt(0).toUpperCase() + action.slice(1)} User?`,
            html: `Are you sure you want to ${action} user <strong>${username}</strong>?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: isActive ? '#dc3545' : '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: `Yes, ${action} it!`,
            cancelButtonText: 'Cancel'
        }).then((result) => {
            if (result.isConfirmed) {
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = `/devtools/users/${action}/${userId}`;
                document.body.appendChild(form);
                form.submit();
            }
        });
    },

    confirmResetPassword() {
        const newPassword     = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        const username        = document.getElementById('resetUsername').textContent;

        if (!newPassword || !confirmPassword) {
            DevToolsUtils.showValidationError('Please fill in all password fields');
            return;
        }

        if (newPassword.length < 6) {
            DevToolsUtils.showValidationError('Password must be at least 6 characters');
            return;
        }

        if (newPassword !== confirmPassword) {
            Swal.fire({
                icon: 'error',
                title: 'Passwords Do Not Match',
                text: 'Please make sure both passwords match',
                confirmButtonColor: '#E8392F'
            });
            return;
        }

        Swal.fire({
            title: 'Reset Password?',
            html: `Are you sure you want to reset the password for <strong>${username}</strong>?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#E8392F',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Yes, reset it!',
            cancelButtonText: 'Cancel'
        }).then((result) => {
            if (result.isConfirmed) {
                document.getElementById('resetPasswordForm').submit();
            }
        });
    }
};

// Global functions for HTML onclick handlers
function openAddUserModal()        { DevToolsUsers.openAddModal(); }
function closeAddUserModal()       { DevToolsUsers.closeAddModal(); }
function closeEditModal()          { DevToolsUsers.closeEditModal(); }
function closeResetPasswordModal() { DevToolsUsers.closeResetPasswordModal(); }
function confirmAddUser()          { DevToolsUsers.confirmAddUser(); }
function confirmEditUser()         { DevToolsUsers.confirmEditUser(); }
function confirmArchiveUser()      { DevToolsUsers.confirmArchiveUser(); }
function confirmResetPassword()    { DevToolsUsers.confirmResetPassword(); }