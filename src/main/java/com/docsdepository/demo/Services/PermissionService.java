package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for document-access permission checks.
 *
 * Rules (match JPQL queries in ImportableInformationRepository exactly):
 *   IVG 1 = Everyone       → always visible to all active users
 *   IVG 2 = Area           → viewer's area must match doc's targetArea
 *                            (falls back to uploader's area when targetArea is null)
 *   IVG 3 = Office         → viewer's office must match doc's targetOffice
 *                            (falls back to uploader's office when targetOffice is null)
 *   IVG 4 = Department     → viewer must be in same office as targetOffice
 *                            AND in same department as targetDepartment
 *                            (if targetDepartment is null, any user in the office can see it)
 *   IVG 5 = Custom Group   → viewer must be an explicit member of the custom group
 *
 * Admins bypass all checks.
 * Document owners can always access their own documents.
 * Archived documents are inaccessible to non-owners / non-admins.
 */
@Service
public class PermissionService {

    /**
     * Returns true if {@code viewer} is permitted to access {@code doc}.
     */
    public boolean canUserAccessDocument(Users viewer, ImportableInformation doc) {
        if (viewer == null || doc == null) return false;

        // Admins see everything
        if (isAdmin(viewer)) return true;

        // Owners always see their own documents (including archived)
        if (doc.getUploadedBy() != null &&
                doc.getUploadedBy().getUserId().equals(viewer.getUserId())) {
            return true;
        }

        // Archived documents are not accessible to anyone else
        if (Boolean.TRUE.equals(doc.getIsArchived())) return false;

        if (doc.getIntendedViewerGroup() == null) return false;
        int groupId = doc.getIntendedViewerGroup().getId();

        // Group 1: Everyone
        if (groupId == 1) return true;

        // Groups 2-4 all require the viewer to have an office + area
        if (viewer.getOffice() == null || viewer.getOffice().getArea() == null) return false;

        // Resolve effective target fields with uploader fallback (mirrors COALESCE in JPQL)
        Users uploader = doc.getUploadedBy();

        // Group 2: Same Area
        if (groupId == 2) {
            Integer targetAreaId = doc.getTargetArea() != null
                    ? doc.getTargetArea().getId()
                    : (uploader != null && uploader.getOffice() != null && uploader.getOffice().getArea() != null
                            ? uploader.getOffice().getArea().getId()
                            : null);
            if (targetAreaId == null) return false;
            return targetAreaId.equals(viewer.getOffice().getArea().getId());
        }

        // Group 3: Same Office
        if (groupId == 3) {
            Integer targetOfficeId = doc.getTargetOffice() != null
                    ? doc.getTargetOffice().getOfficeId()
                    : (uploader != null && uploader.getOffice() != null
                            ? uploader.getOffice().getOfficeId()
                            : null);
            if (targetOfficeId == null) return false;
            return targetOfficeId.equals(viewer.getOffice().getOfficeId());
        }

        // Group 4: Same Office + Department
        if (groupId == 4) {
            Integer targetOfficeId = doc.getTargetOffice() != null
                    ? doc.getTargetOffice().getOfficeId()
                    : (uploader != null && uploader.getOffice() != null
                            ? uploader.getOffice().getOfficeId()
                            : null);
            if (targetOfficeId == null) return false;
            if (!targetOfficeId.equals(viewer.getOffice().getOfficeId())) return false;

            // If no specific department was targeted, anyone in the office can see it
            if (doc.getTargetDepartment() == null) return true;

            // Otherwise viewer must be in the exact target department
            return viewer.getDepartment() != null &&
                   doc.getTargetDepartment().getId().equals(viewer.getDepartment().getId());
        }

        // Group 5: Custom Group — viewer must be an explicit member
        if (groupId == 5) {
            if (doc.getCustomViewerGroup() == null) return false;
            return doc.getCustomViewerGroup().getMembers()
                    .stream()
                    .anyMatch(m -> m.getUserId().equals(viewer.getUserId()));
        }

        return false;
    }

    /**
     * Returns true if the user has the admin role.
     */
    public boolean isAdmin(Users user) {
        return user != null && user.getRole() != null &&
               (user.getRole().getId() == 1 ||
                "ADMIN".equalsIgnoreCase(user.getRole().getName()));
    }
}