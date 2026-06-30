/**
 * document-modal.js
 */

function _cfg() { return window.modalConfig || {}; }

let currentDocumentId       = null;
let _ownerIdOfOpenDoc       = null;
let isEditMode              = false;
let originalDocumentData    = {};
let currentDocumentIsBroken = false;

function _isOwner() {
    return _cfg().currentUserId != null &&
           _ownerIdOfOpenDoc    != null &&
           String(_cfg().currentUserId) === String(_ownerIdOfOpenDoc);
}

function canEdit() {
    if (_cfg().pageMode === 'archived') return false;
    return _isOwner() || !!_cfg().isAdmin;
}

function _applyOwnerButtons() {
    const allowed = canEdit();

    const btnArchive = document.getElementById('btnArchiveModal');
    if (btnArchive) btnArchive.style.display = allowed ? '' : 'none';

    const btnUpload = document.getElementById('btnUploadVersionModal');
    if (btnUpload) btnUpload.style.display = allowed ? '' : 'none';

    const btnEdit = document.getElementById('btnEditMode');
    if (btnEdit) btnEdit.style.display = allowed ? 'inline-block' : 'none';

    const vhSection = document.getElementById('versionHistorySection');
    if (vhSection) vhSection.style.display = allowed ? '' : 'none';

    if (!allowed) {
        _hide('btnSaveChanges');
        _hide('btnCancelEdit');
    }
}

// ─── open / close ─────────────────────────────────────────────────────────

async function openDocumentModal(documentId) {
    currentDocumentId = documentId;
    _show('documentViewerModal');

    try {
        const data = await _fetchJSON(`/documents/details/${documentId}`);
        _ownerIdOfOpenDoc = data.uploadedById;

        if (_cfg().pageMode === 'archived') {
            const fe = await _fetchJSON(`/api/documents/${documentId}/file-exists`);
            currentDocumentIsBroken = !fe.exists;
            _applyBrokenState(currentDocumentIsBroken);
        } else {
            currentDocumentIsBroken = false;
        }

        _setText('modalDocTitle',      data.title);
        _setText('infoFilename',       data.filename);
        _setText('infoUploadedBy',     data.uploadedBy);
        _setText('infoUploadDate',     _fmtDate(data.uploadDate));
        _setText('infoClassification', data.documentClassification);
        _setText('infoViewerGroup',    data.intendedViewerGroup);
        _setText('infoDescription',    data.description || 'No description');
        _setText('infoDtsNumber',      data.dtsNumber   || '-');

        _setText('infoTargetOffice',     data.targetOfficeName     || '-');
        _setText('infoTargetDepartment', data.targetDepartmentName || 'Entire office');

        if (_cfg().pageMode === 'archived') {
            _setText('infoArchivedDate', _fmtDate(data.dateCreated));
            _setText('infoArchivedBy',   data.uploadedBy);
            const statusEl = document.getElementById('infoFileStatus');
            if (statusEl) {
                statusEl.innerHTML = currentDocumentIsBroken
                    ? '<span class="badge bg-danger"><i class="fas fa-unlink"></i> Broken Link</span>'
                    : '<span class="badge bg-success"><i class="fas fa-check"></i> OK</span>';
            }
            const btnRestore = document.getElementById('btnRestoreModal');
            if (btnRestore) {
                const canRestore = _isOwner() || !!_cfg().isAdmin;
                btnRestore.style.display = canRestore ? '' : 'none';
                btnRestore.disabled      = currentDocumentIsBroken;
                btnRestore.title         = currentDocumentIsBroken
                    ? 'Cannot restore – file not found in storage'
                    : 'Restore document';
            }
        }

        const tagsDiv = document.getElementById('infoTags');
        if (tagsDiv) {
            const names = data.tagNames
                || (data.tags || []).map(t => (typeof t === 'string' ? t : t.tagName));
            tagsDiv.innerHTML = names.length
                ? names.map(n =>
                    `<span class="badge"
                           style="background-color:#fcebea;color:#E8392F;
                                  font-size:0.75rem;margin-right:0.25rem;">
                         <i class="fas fa-tag"></i> ${_esc(n)}
                     </span>`).join('')
                : 'No tags';
        }

        // ── Contract dates ────────────────────────────────────────────────
        populateContractDates(
            data.documentClassification,
            data.contractStartDate,
            data.contractEndDate
        );

        populateEditFields(data);
        _applyOwnerButtons();
        cancelEditMode();
        loadComments(data.comments, data.currentUserId);
        _setText('commentCount', data.commentCount ?? (data.comments || []).length);

        if (canEdit()) {
            loadVersionHistory(documentId);
        }

    } catch (err) {
        console.error('openDocumentModal error:', err);
        Swal.fire({ icon: 'error', title: 'Error',
            text: 'Failed to load document details.',
            confirmButtonColor: '#E8392F' });
        closeDocumentModal();
    }
}

function closeDocumentModal() {
    _hide('documentViewerModal');
    currentDocumentId       = null;
    _ownerIdOfOpenDoc       = null;
    currentDocumentIsBroken = false;
    if (isEditMode) cancelEditMode();
}

// ─── primary actions ──────────────────────────────────────────────────────

function viewDocumentNewTab() {
    if (currentDocumentId && !currentDocumentIsBroken) {
        window.open(`/documents/view/${currentDocumentId}`, '_blank');
    }
}

function downloadDocument(docId) {
    const id = docId || currentDocumentId;
    if (!id || currentDocumentIsBroken) return;

    if (_cfg().pageMode === 'archived') {
        window.location.href = `/documents/download/${id}`;
        return;
    }

    fetch(`/documents/download/${id}`)
        .then(r => r.blob())
        .then(blob => {
            const url  = URL.createObjectURL(blob);
            const link = Object.assign(document.createElement('a'), {
                href    : url,
                download: _getTextContent('infoFilename') || 'document'
            });
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        })
        .catch(() => Swal.fire({
            icon: 'error', title: 'Download Failed',
            text: 'Could not download the file. Please try again.',
            confirmButtonColor: '#E8392F'
        }));
}

function archiveDocument() {
    if (!canEdit()) {
        Swal.fire({ icon: 'error', title: 'Access Denied',
            text: 'Only the document owner or an administrator can archive documents.',
            confirmButtonColor: '#E8392F' });
        return;
    }
    if (!currentDocumentId) return;

    Swal.fire({
        title: 'Archive this document?',
        html: `<p>This document will be moved to the archive and will no longer
                   appear in active documents.</p>
               <p><strong>Note:</strong> Archived documents can be restored by
                   the owner or an administrator.</p>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#ff9800',
        cancelButtonColor:  '#6c757d',
        confirmButtonText: '<i class="fas fa-archive me-2"></i>Yes, archive it',
        cancelButtonText: 'Cancel'
    }).then(result => {
        if (!result.isConfirmed) return;
        fetch(`/documents/archive/${currentDocumentId}`, { method: 'POST' })
            .then(_requireOk)
            .then(() => Swal.fire({
                icon: 'success', title: 'Archived!',
                text: 'Document has been archived successfully.',
                confirmButtonColor: '#E8392F',
                timer: 2000, showConfirmButton: false
            }).then(() => location.reload()))
            .catch(err => Swal.fire({
                icon: 'error', title: 'Error',
                text: err.message || 'Failed to archive document. Please try again.',
                confirmButtonColor: '#E8392F'
            }));
    });
}

// ─── restore ──────────────────────────────────────────────────────────────

function restoreDocumentFromModal() {
    if (currentDocumentIsBroken) {
        Swal.fire({ icon: 'error', title: 'Cannot Restore',
            text: 'This document cannot be restored because the file is missing from storage.',
            confirmButtonColor: '#dc3545' });
        return;
    }
    confirmRestore(currentDocumentId, _getTextContent('modalDocTitle'));
}

function confirmRestore(documentId, documentTitle) {
    Swal.fire({
        title: 'Restore Document?',
        html: `<p>Are you sure you want to restore:</p>
               <p><strong>${_esc(documentTitle)}</strong></p>
               <p class="text-info small">
                   <i class="fas fa-info-circle"></i>
                   This will move the document back to active files.
               </p>`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor:  '#6c757d',
        confirmButtonText: '<i class="fas fa-undo"></i> Yes, Restore',
        cancelButtonText: 'Cancel'
    }).then(result => result.isConfirmed && _doRestore(documentId, documentTitle));
}

async function _doRestore(documentId, documentTitle) {
    Swal.fire({ title: 'Restoring…', allowOutsideClick: false,
        didOpen: () => Swal.showLoading() });
    try {
        const data = await _fetchJSON(`/archived/restore/${documentId}`, { method: 'POST' });
        if (data.success) {
            await Swal.fire({
                icon: 'success', title: 'Restored!',
                html: `<p><strong>${_esc(documentTitle)}</strong> has been restored.</p>
                       <p class="text-muted small">The document is now in your active files.</p>`,
                confirmButtonColor: '#198754'
            });
            location.reload();
        } else {
            Swal.fire({ icon: 'error', title: 'Restore Failed',
                text: data.message || 'Failed to restore document.',
                confirmButtonColor: '#dc3545' });
        }
    } catch {
        Swal.fire({ icon: 'error', title: 'Error',
            text: 'An error occurred while restoring the document.',
            confirmButtonColor: '#dc3545' });
    }
}

// ─── comments ─────────────────────────────────────────────────────────────

function loadComments(comments, currentUserId) {
    const list = document.getElementById('commentsList');
    if (!list) return;
    list.innerHTML = '';

    if (!comments || !comments.length) {
        list.innerHTML = `<div class="no-comments">${
            _cfg().pageMode === 'archived'
                ? 'No comments.'
                : 'No comments yet. Be the first to comment!'
        }</div>`;
        return;
    }

    comments.forEach(c => {
        const canDelete = _cfg().pageMode !== 'archived' &&
                          c.userId != null &&
                          String(currentUserId) === String(c.userId);
        const div = document.createElement('div');
        div.className = 'comment-item';
        div.innerHTML = `
            <div class="comment-author">
                <i class="fas fa-user-circle"></i>
                ${_esc(c.username)}
                <span class="comment-date">${_fmtDate(c.createdAt)}</span>
                ${canDelete
                    ? `<button class="btn-delete-comment"
                               onclick="deleteComment(${c.commentId})"
                               title="Delete comment">
                           <i class="fas fa-trash-alt"></i>
                       </button>`
                    : ''}
            </div>
            <div class="comment-text">${_esc(c.commentText)}</div>`;
        list.appendChild(div);
    });
}

function addComment() {
    const textarea = document.getElementById('commentText');
    const text     = (textarea && textarea.value || '').trim();

    if (!text) {
        Swal.fire({ icon: 'warning', title: 'Empty Comment',
            text: 'Please enter a comment before posting.',
            confirmButtonColor: '#E8392F' });
        return;
    }

    const fd = new FormData();
    fd.append('commentText', text);

    fetch(`/documents/${currentDocumentId}/comments`, { method: 'POST', body: fd })
        .then(_requireOk)
        .then(() => {
            textarea.value = '';
            Swal.fire({ icon: 'success', title: 'Comment Added',
                text: 'Your comment has been posted.',
                confirmButtonColor: '#E8392F',
                timer: 1500, showConfirmButton: false });
            openDocumentModal(currentDocumentId);
        })
        .catch(() => Swal.fire({ icon: 'error', title: 'Error',
            text: 'Failed to add comment. Please try again.',
            confirmButtonColor: '#E8392F' }));
}

function deleteComment(commentId) {
    Swal.fire({
        title: 'Delete this comment?',
        text: 'This action cannot be undone.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor:  '#6c757d',
        confirmButtonText: '<i class="fas fa-trash-alt me-2"></i>Yes, delete it',
        cancelButtonText: 'Cancel'
    }).then(result => {
        if (!result.isConfirmed) return;
        fetch(`/documents/comments/${commentId}`, { method: 'DELETE' })
            .then(_requireOk)
            .then(() => Swal.fire({
                icon: 'success', title: 'Deleted!',
                text: 'Comment has been deleted.',
                confirmButtonColor: '#E8392F',
                timer: 1500, showConfirmButton: false
            }).then(() => openDocumentModal(currentDocumentId)))
            .catch(() => Swal.fire({ icon: 'error', title: 'Error',
                text: 'Failed to delete comment. Please try again.',
                confirmButtonColor: '#E8392F' }));
    });
}

// ─── edit mode ────────────────────────────────────────────────────────────

function toggleEditMode() {
    if (!canEdit()) return;
    isEditMode = true;

    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'block');

    _hide('btnEditMode');
    _show('btnSaveChanges', 'inline-block');
    _show('btnCancelEdit',  'inline-block');

    // Apply correct sub-field visibility based on current viewer group
    _applyViewerGroupEditState();

    // Show contract date edit rows if currently a contract classification
    toggleContractDatesEdit();

    // Seed the target office dropdown and populate depts
    if (_cfg().isAdmin) {
        const officeId = originalDocumentData.targetOfficeId || '';
        _setVal('editTargetOffice', officeId);
        _populateModalDepartments(officeId, originalDocumentData.targetDepartmentId || '');
    }
}

function cancelEditMode() {
    isEditMode = false;

    _setVal('editClassification', originalDocumentData.classificationId    || '');
    _setVal('editTags',           originalDocumentData.tags                 || '');
    _setVal('editViewerGroup',    originalDocumentData.viewerGroupId        || '1');
    _setVal('editCustomGroup',    originalDocumentData.customGroupId        || '');
    _setVal('editDescription',    originalDocumentData.description          || '');
    _setVal('editDtsNumber',      originalDocumentData.dtsNumber            || '');
    _setVal('editContractStart',  originalDocumentData.contractStartDate    || '');
    _setVal('editContractEnd',    originalDocumentData.contractEndDate      || '');

    if (_cfg().isAdmin) {
        _setVal('editTargetOffice', originalDocumentData.targetOfficeId     || '');
        _setVal('editTargetDept',   originalDocumentData.targetDepartmentId || '');
    }

    document.querySelectorAll('.view-mode').forEach(el => el.style.display = 'block');
    document.querySelectorAll('.edit-mode').forEach(el => el.style.display = 'none');

    if (canEdit()) {
        _show('btnEditMode', 'inline-block');
    }
    _hide('btnSaveChanges');
    _hide('btnCancelEdit');
    _hide('editCustomGroupContainer');
    _hide('editTargetOrgBlock');

    // Re-apply contract row visibility based on the original (view-mode) classification
    populateContractDates(
        originalDocumentData.classificationName  || '',
        originalDocumentData.contractStartDate   || null,
        originalDocumentData.contractEndDate     || null
    );
}

/**
 * Called whenever the viewer group dropdown changes in edit mode.
 * Controls visibility of Target Org block and Custom Group selector.
 *
 * Rules (mirrors the upload page):
 *   1 = Area, 2 = Office, 3 = All/Public, 4 = Department, 5 = Custom
 *   Area / Office / Dept  → show Target Org block
 *   Dept only             → also show Target Department column inside it
 *   All / Custom          → hide Target Org block entirely
 *   Custom                → show Custom Group selector
 */
function toggleCustomGroupEdit() {
    _applyViewerGroupEditState();
}

function _applyViewerGroupEditState() {
    const val = _getVal('editViewerGroup');

    const isArea   = val === '2';
    const isOffice = val === '3';
    const isDept   = val === '4';
    const isCustom = val === '5';
    const isOrgGroup = isArea || isOffice || isDept;

    // Target Org block — only for Area / Office / Department
    if (_cfg().isAdmin) {
        const orgBlock = document.getElementById('editTargetOrgBlock');
        if (orgBlock) orgBlock.style.display = isOrgGroup ? '' : 'none';

        // Department column inside the org block
        const deptCol = document.getElementById('editTargetDeptCol');
        if (deptCol) deptCol.style.display = isDept ? '' : 'none';
    }

    // Custom group selector
    const cgContainer = document.getElementById('editCustomGroupContainer');
    const cgSelect    = document.getElementById('editCustomGroup');
    if (cgContainer) cgContainer.style.display = isCustom ? 'block' : 'none';
    if (cgSelect && !isCustom) cgSelect.value = '';
}

/**
 * Called when the admin changes Target Office in the edit modal.
 * Re-populates departments and refreshes visibility state.
 */
function onModalTargetOfficeChange() {
    const officeId = _getVal('editTargetOffice');
    _populateModalDepartments(officeId, '');
}

/**
 * Fills editTargetDept from window.modalDepartments, pre-selecting selectedDeptId.
 */
function _populateModalDepartments(officeId, selectedDeptId) {
    const deptSel = document.getElementById('editTargetDept');
    if (!deptSel) return;

    deptSel.innerHTML = '<option value="">-- Entire office --</option>';

    const depts = (window.modalDepartments || {})[officeId] || [];
    depts.forEach(function(d) {
        const opt = document.createElement('option');
        opt.value       = d.id;
        opt.textContent = d.name;
        if (String(d.id) === String(selectedDeptId)) opt.selected = true;
        deptSel.appendChild(opt);
    });
}

function saveDocumentChanges() {
    if (!canEdit()) return;

    const classificationId = _getVal('editClassification');
    const tags             = _getVal('editTags');
    const viewerGroupId    = _getVal('editViewerGroup');
    const customGroupId    = _getVal('editCustomGroup') || '';
    const description      = _getVal('editDescription');
    const dtsNumber        = _getVal('editDtsNumber');
    const targetOfficeId   = _cfg().isAdmin ? (_getVal('editTargetOffice') || '') : '';
    const targetDeptId     = _cfg().isAdmin ? (_getVal('editTargetDept')   || '') : '';

    if (!classificationId) {
        Swal.fire({ icon: 'warning', title: 'Missing Classification',
            text: 'Please select a classification.',
            confirmButtonColor: '#E8392F' });
        return;
    }
    if (viewerGroupId === '5' && !customGroupId) {
        Swal.fire({ icon: 'warning', title: 'Missing Custom Group',
            text: 'Please select a custom group when "Custom Group" is chosen.',
            confirmButtonColor: '#E8392F' });
        return;
    }
    if (_cfg().isAdmin && ['1','2','4'].includes(viewerGroupId) && !targetOfficeId) {
        Swal.fire({ icon: 'warning', title: 'Target Office Required',
            text: 'Please select a Target Office for the chosen viewer group.',
            confirmButtonColor: '#E8392F' });
        return;
    }

    // ── Contract date validation ──────────────────────────────────────────
    const editClassSel    = document.getElementById('editClassification');
    const editClassText   = editClassSel?.options[editClassSel.selectedIndex]?.text || '';
    const savingIsContract = editClassText.toLowerCase().includes('contract');
    const contractValidation = validateContractDatesEdit(savingIsContract);

    if (!contractValidation.valid) {
        Swal.fire({ icon: 'warning', title: 'Contract Dates Required',
            text: contractValidation.message,
            confirmButtonColor: '#E8392F' });
        return;
    }

    Swal.fire({
        title: 'Save Changes?',
        text: 'Are you sure you want to update this document?',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#28a745',
        cancelButtonColor:  '#6c757d',
        confirmButtonText: '<i class="fas fa-save me-2"></i>Yes, save changes',
        cancelButtonText: 'Cancel'
    }).then(result => {
        if (result.isConfirmed) {
            _doDocumentUpdate(
                classificationId, tags, viewerGroupId, customGroupId,
                description, dtsNumber, targetOfficeId, targetDeptId,
                contractValidation.startDate, contractValidation.endDate
            );
        }
    });
}

function _doDocumentUpdate(classificationId, tags, viewerGroupId, customGroupId,
                           description, dtsNumber, targetOfficeId, targetDeptId,
                           contractStartDate, contractEndDate) {
    Swal.fire({ title: 'Saving…', allowOutsideClick: false,
        didOpen: () => Swal.showLoading() });

    const fd = new FormData();
    fd.append('classificationId', classificationId);
    fd.append('tags',             tags);
    fd.append('viewerGroupId',    viewerGroupId);
    if (customGroupId)   fd.append('customGroupId',    customGroupId);
    fd.append('description', description);
    fd.append('dtsNumber',   dtsNumber || '');

    if (_cfg().isAdmin) {
        if (targetOfficeId) fd.append('targetOfficeId',     targetOfficeId);
        if (targetDeptId)   fd.append('targetDepartmentId', targetDeptId);
    }

    // Contract dates — always sent so server can clear them when switching away from contract
    fd.append('contractStartDate', contractStartDate || '');
    fd.append('contractEndDate',   contractEndDate   || '');

    fetch(`/documents/${currentDocumentId}/update`, { method: 'POST', body: fd })
        .then(r => r.ok
            ? r.json()
            : r.json().then(e => Promise.reject(e.message || 'Failed to update document')))
        .then(() => Swal.fire({
            icon: 'success', title: 'Updated!',
            text: 'Document has been updated successfully.',
            confirmButtonColor: '#E8392F',
            timer: 2000, showConfirmButton: false
        }).then(() => location.reload()))
        .catch(msg => Swal.fire({ icon: 'error', title: 'Update Failed',
            text: msg || 'Failed to update document. Please try again.',
            confirmButtonColor: '#E8392F' }));
}

function populateEditFields(data) {
    const tagsString = (
        data.tagNames ||
        (data.tags || []).map(t => (typeof t === 'string' ? t : t.tagName))
    ).join(', ');

    // Normalise contract dates — the server returns LocalDate serialised as
    // an array [yyyy, mm, dd] OR a string "yyyy-MM-dd". Convert both to string.
    const normDate = v => {
        if (!v) return '';
        if (Array.isArray(v)) {
            const [y, m, d] = v;
            return `${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
        }
        return String(v).substring(0, 10);
    };

    originalDocumentData = {
        classificationId    : String(data.classificationId   || ''),
        classificationName  : data.documentClassification    || '',
        tags                : tagsString,
        viewerGroupId       : String(data.viewerGroupId       || '1'),
        customGroupId       : String(data.customGroupId       || ''),
        description         : data.description                || '',
        dtsNumber           : data.dtsNumber                  || '',
        targetOfficeId      : String(data.targetOfficeId      || ''),
        targetDepartmentId  : String(data.targetDepartmentId  || ''),
        contractStartDate   : normDate(data.contractStartDate),
        contractEndDate     : normDate(data.contractEndDate)
    };

    _setVal('editClassification', originalDocumentData.classificationId);
    _setVal('editTags',           originalDocumentData.tags);
    _setVal('editViewerGroup',    originalDocumentData.viewerGroupId);
    _setVal('editCustomGroup',    originalDocumentData.customGroupId);
    _setVal('editDescription',    originalDocumentData.description);
    _setVal('editDtsNumber',      originalDocumentData.dtsNumber);
    _setVal('editTargetOffice',   originalDocumentData.targetOfficeId);
    _setVal('editTargetDept',     originalDocumentData.targetDepartmentId);
    _setVal('editContractStart',  originalDocumentData.contractStartDate);
    _setVal('editContractEnd',    originalDocumentData.contractEndDate);

    // Department option restriction
    const viewerGroupSelect = document.getElementById('editViewerGroup');
    const deptWarning       = document.getElementById('deptWarning');

    if (viewerGroupSelect) {
        const deptOption = viewerGroupSelect.querySelector('option[value="4"]');
        if (deptOption) {
            if (!data.uploaderHasDepartment && !_cfg().isAdmin) {
                deptOption.disabled = true;
                deptOption.title    = 'Uploader is not assigned to a department';
                if (originalDocumentData.viewerGroupId === '4') {
                    _setVal('editViewerGroup', '1');
                    originalDocumentData.viewerGroupId = '1';
                }
                if (deptWarning) deptWarning.style.display = 'block';
            } else {
                deptOption.disabled = false;
                deptOption.title    = '';
                if (deptWarning) deptWarning.style.display = 'none';
            }
        }
    }
}

// ─── version history ──────────────────────────────────────────────────────

function loadVersionHistory(documentId) {
    fetch(`/documents/${documentId}/versions`)
        .then(r => r.json())
        .then(versions => {
            const list = document.getElementById('versionHistoryList');
            if (!list) return;
            _setText('versionCount', versions.length);

            if (!versions.length) {
                list.innerHTML = '<p class="text-muted text-center">No previous versions</p>';
                return;
            }

            list.innerHTML = versions.map(v => `
                <div class="version-item">
                    <div>
                        <span class="version-badge">v${v.versionNumber}</span>
                        ${v.isArchived
                            ? '<span class="badge bg-secondary ms-2">Archived</span>'
                            : ''}
                    </div>
                    <div class="mt-1">
                        <strong>${_esc(v.originalFilename)}</strong><br>
                        <small class="text-muted">
                            Uploaded by ${_esc(v.uploadedBy)}<br>
                            ${_fmtDate(v.uploadDate)}
                            ${v.versionNotes ? `<br>Notes: ${_esc(v.versionNotes)}` : ''}
                        </small>
                    </div>
                    <div class="mt-2">
                        <button class="btn btn-sm btn-outline-primary"
                                onclick="viewVersion(${v.documentId})">
                            <i class="fas fa-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-secondary"
                                onclick="downloadVersion(${v.documentId})">
                            <i class="fas fa-download"></i>
                        </button>
                    </div>
                </div>`).join('');
        })
        .catch(err => console.error('Error loading version history:', err));
}

function viewVersion(documentId) {
    window.open(`/documents/view/${documentId}`, '_blank');
}

function downloadVersion(documentId) {
    window.open(`/documents/download/${documentId}`, '_blank');
}

function openUploadVersionModal() {
    if (!canEdit()) {
        Swal.fire({ icon: 'error', title: 'Access Denied',
            text: 'Only the document owner or an administrator can upload new versions.',
            confirmButtonColor: '#E8392F' });
        return;
    }
    if (!currentDocumentId) return;
    _setVal('versionDocumentId', currentDocumentId);
    _show('uploadVersionModal');
}

function closeUploadVersionModal() {
    _hide('uploadVersionModal');
    const form = document.getElementById('uploadVersionForm');
    if (form) form.reset();
}

function _confirmAndUploadVersion(documentId, file, notes) {
    Swal.fire({
        title: 'Upload New Version?',
        html: `<div class="text-start">
                   <p><strong>File:</strong> ${_esc(file.name)}</p>
                   <p><strong>Size:</strong> ${_fmtSize(file.size)}</p>
                   ${notes ? `<p><strong>Notes:</strong> ${_esc(notes)}</p>` : ''}
                   <hr>
                   <p class="text-warning">
                       <i class="fas fa-exclamation-triangle me-2"></i>
                       The current file will be archived and replaced.
                   </p>
                   <p class="text-muted"><strong>This action cannot be undone.</strong></p>
               </div>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#E8392F',
        cancelButtonColor:  '#6c757d',
        confirmButtonText: '<i class="fas fa-upload me-2"></i>Yes, upload new version',
        cancelButtonText: 'Cancel'
    }).then(result => result.isConfirmed && _doUploadVersion(documentId, file, notes));
}

function _doUploadVersion(documentId, file, notes) {
    Swal.fire({
        title: 'Uploading…',
        html: `<p>Please wait while we upload the new version.</p>
               <div class="progress mt-3">
                   <div class="progress-bar progress-bar-striped progress-bar-animated"
                        style="width:100%"></div>
               </div>`,
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => Swal.showLoading()
    });

    const fd = new FormData();
    fd.append('file', file);
    if (notes) fd.append('versionNotes', notes);

    fetch(`/documents/${documentId}/upload-new-version`, { method: 'POST', body: fd })
        .then(r => r.ok
            ? r.json()
            : r.json().then(e => Promise.reject(e.message || 'Upload failed')))
        .then(data => {
            if (data.status !== 'success') throw data.message || 'Upload failed';
            Swal.fire({
                icon: 'success', title: 'Success!',
                html: `<p>Version ${data.versionNumber} uploaded successfully!</p>
                       <p class="text-muted">The previous version has been archived.</p>`,
                confirmButtonColor: '#E8392F',
                timer: 3000
            }).then(() => { closeUploadVersionModal(); location.reload(); });
        })
        .catch(msg => Swal.fire({ icon: 'error', title: 'Upload Failed',
            html: `<p>${_esc(String(msg))}</p>
                   <p class="text-muted mt-2">Please try again or contact support.</p>`,
            confirmButtonColor: '#E8392F' }));
}

// ─── export ───────────────────────────────────────────────────────────────

function confirmExport() {
    const ex = _cfg().export || {};

    if (!ex.totalItems) {
        Swal.fire({ icon: 'warning', title: 'No Data',
            text: 'There are no documents to export.' });
        return;
    }

    Swal.fire({
        title: 'Export to Excel?',
        html: `<p>Export <strong>${ex.totalItems}</strong> document(s) to Excel?</p>
               ${ex.query
                   ? `<p class="text-muted small">Search: "${_esc(ex.query)}"</p>`
                   : ''}`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#198754',
        cancelButtonColor:  '#6c757d',
        confirmButtonText: '<i class="fas fa-file-excel"></i> Export',
        cancelButtonText: 'Cancel'
    }).then(result => result.isConfirmed && _doExport());
}

async function _doExport() {
    const ex   = _cfg().export || {};
    const mode = _cfg().pageMode;

    const params = new URLSearchParams();
    if (ex.query)              params.append('query',              ex.query);
    if (ex.classificationId)   params.append('classificationId',   ex.classificationId);
    if (ex.uploadedById)       params.append('uploadedById',        ex.uploadedById);
    if (ex.startDate)          params.append('startDate',           ex.startDate);
    if (ex.endDate)            params.append('endDate',             ex.endDate);
    if (ex.filterAreaId)       params.append('filterAreaId',        ex.filterAreaId);
    if (ex.filterOfficeId)     params.append('filterOfficeId',      ex.filterOfficeId);
    if (ex.filterDepartmentId) params.append('filterDepartmentId',  ex.filterDepartmentId);
    if (ex.customGroupId)      params.append('customGroupId',       ex.customGroupId);

    const baseUrl = mode === 'shared'   ? '/shared/export'
                  : mode === 'archived' ? '/archived/export'
                                        : '/myfiles/export';

    Swal.fire({
        title: 'Generating Excel…',
        html: `<p>Preparing ${ex.totalItems} records…</p>
               <div class="progress mt-3">
                   <div class="progress-bar progress-bar-striped progress-bar-animated"
                        style="width:100%"></div>
               </div>`,
        allowOutsideClick: false,
        allowEscapeKey: false,
        showConfirmButton: false,
        didOpen: () => Swal.showLoading()
    });

    try {
        const response = await fetch(`${baseUrl}?${params}`);
        if (!response.ok) throw new Error('Export request failed');

        const blob        = await response.blob();
        const disposition = response.headers.get('Content-Disposition') || '';
        const filename    = disposition.includes('filename=')
            ? disposition.split('filename=')[1].replace(/"/g, '')
            : 'export.xlsx';

        const link = Object.assign(document.createElement('a'), {
            href: URL.createObjectURL(blob), download: filename
        });
        document.body.appendChild(link);
        link.click();
        link.remove();

        Swal.fire({ icon: 'success', title: 'Export Complete',
            text: 'Your Excel file has been downloaded.',
            timer: 2000, showConfirmButton: false });

    } catch {
        Swal.fire({ icon: 'error', title: 'Export Failed',
            text: 'Unable to generate the Excel file. Please try again.' });
    }
}

// ─── broken-link state ────────────────────────────────────────────────────

function _applyBrokenState(isBroken) {
    _toggleDisplay('modalBrokenBadge', isBroken, 'inline-block');
    _toggleDisplay('brokenLinkAlert',  isBroken, 'block');

    const btnView     = document.getElementById('btnViewModal');
    const btnDownload = document.getElementById('btnDownloadModal');
    if (btnView)     btnView.disabled     = isBroken;
    if (btnDownload) btnDownload.disabled = isBroken;
}

// ─── DOM utilities ────────────────────────────────────────────────────────

function _show(id, display) {
    const el = document.getElementById(id);
    if (el) el.style.display = display || 'block';
}
function _hide(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}
function _toggleDisplay(id, condition, whenTrue) {
    const el = document.getElementById(id);
    if (el) el.style.display = condition ? (whenTrue || 'block') : 'none';
}
function _setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = (text != null) ? text : '';
}
function _setVal(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = (val != null) ? val : '';
}
function _getVal(id) {
    return (document.getElementById(id) || {}).value || '';
}
function _getTextContent(id) {
    const el = document.getElementById(id);
    return el ? el.textContent.trim() : '';
}

// ─── format helpers ───────────────────────────────────────────────────────

function _fmtDate(ds) {
    if (!ds) return '-';
    const d = new Date(ds);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
}
function _fmtSize(bytes) {
    if (!bytes) return '0 Bytes';
    const k = 1024, sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
}
function _esc(text) {
    if (!text) return '';
    const d = document.createElement('div');
    d.textContent = String(text);
    return d.innerHTML;
}

// ─── fetch helpers ────────────────────────────────────────────────────────

async function _fetchJSON(url, opts) {
    const r = await fetch(url, opts || {});
    if (!r.ok) throw new Error(`HTTP ${r.status} – ${url}`);
    return r.json();
}
function _requireOk(r) {
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.json();
}

// ─── legacy aliases ───────────────────────────────────────────────────────

function formatDate(ds)          { return _fmtDate(ds); }
function escapeHtml(text)        { return _esc(text); }
function formatFileSize(bytes)   { return _fmtSize(bytes); }
function performDocumentUpdate() { /* no-op */ }
function confirmAndUploadVersion(id, file, notes) { _confirmAndUploadVersion(id, file, notes); }
function uploadNewVersion(id, file, notes)        { _doUploadVersion(id, file, notes); }

// ─── event wiring ─────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.btn-view').forEach(function (btn) {
        btn.addEventListener('click', function () {
            openDocumentModal(this.dataset.id);
        });
    });

    document.querySelectorAll('.btn-restore').forEach(function (btn) {
        btn.addEventListener('click', function () {
            if (this.dataset.broken === 'true') {
                Swal.fire({ icon: 'error', title: 'Cannot Restore',
                    text: 'This document cannot be restored because the file is missing from storage.',
                    confirmButtonColor: '#dc3545' });
                return;
            }
            confirmRestore(this.dataset.id, this.dataset.title);
        });
    });

    var uploadForm = document.getElementById('uploadVersionForm');
    if (uploadForm) {
        uploadForm.addEventListener('submit', function (e) {
            e.preventDefault();
            var documentId = _getVal('versionDocumentId');
            var fileInput  = document.getElementById('versionFile');
            var file       = fileInput && fileInput.files[0];
            var notes      = _getVal('versionNotes');

            if (!file) {
                Swal.fire({ icon: 'warning', title: 'No File Selected',
                    text: 'Please select a file to upload.',
                    confirmButtonColor: '#E8392F' });
                return;
            }
            if (file.size > 100 * 1024 * 1024) {
                Swal.fire({ icon: 'warning', title: 'File Too Large',
                    text: 'File size must be less than 100 MB.',
                    confirmButtonColor: '#E8392F' });
                return;
            }
            _confirmAndUploadVersion(documentId, file, notes);
        });
    }

    var docIdToOpen = sessionStorage.getItem('openDocumentModal');
    if (docIdToOpen) {
        sessionStorage.removeItem('openDocumentModal');
        setTimeout(function () {
            var btn = document.querySelector('button.btn-view[data-id="' + docIdToOpen + '"]');
            if (btn) {
                btn.click();
            } else {
                openDocumentModal(parseInt(docIdToOpen, 10));
            }
        }, 300);
    }
});

window.addEventListener('click', function (e) {
    if (e.target === document.getElementById('documentViewerModal'))  closeDocumentModal();
    if (e.target === document.getElementById('uploadVersionModal'))    closeUploadVersionModal();
});

document.addEventListener('keydown', function (e) {
    if (e.key !== 'Escape') return;
    var dm = document.getElementById('documentViewerModal');
    var um = document.getElementById('uploadVersionModal');
    if (dm && dm.style.display === 'block') closeDocumentModal();
    if (um && um.style.display === 'block') closeUploadVersionModal();
});