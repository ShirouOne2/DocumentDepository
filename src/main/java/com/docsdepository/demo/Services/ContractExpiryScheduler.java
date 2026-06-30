package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.CustomViewerGroup;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Runs once a day at 08:00 and fires contract expiry alerts.
 *
 * Recipient rules per viewer group:
 *   - Department  → all active users in targetDepartment
 *   - Office      → all active users in targetOffice
 *   - Area        → all active users in targetOffice (area is too broad)
 *   - Custom Group→ all members of the custom viewer group
 *   - All/Public  → uploader only (blasting everyone system-wide is too noisy)
 *
 * The uploader is always included regardless of viewer group.
 * Each alert fires exactly once per threshold (30-day, 7-day) via boolean flags.
 * Archived documents are skipped entirely.
 */
@Service
public class ContractExpiryScheduler {

    private static final Logger logger =
        LoggerFactory.getLogger(ContractExpiryScheduler.class);

    // Viewer group IDs — must match your intended_viewer_groups table
    private static final int VG_ALL        = 1;
    private static final int VG_AREA       = 2;
    private static final int VG_OFFICE     = 3;
    private static final int VG_DEPARTMENT = 4;
    private static final int VG_CUSTOM     = 5;

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    // ── Run every day at 08:00 ────────────────────────────────────────────────
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkContractExpiry() {

        logger.info("ContractExpiryScheduler: starting daily contract expiry check");

        LocalDate today     = LocalDate.now();
        int processed = 0, alerted = 0;

        List<ImportableInformation> contracts =
            documentRepository.findActiveContractsWithEndDate();

        for (ImportableInformation doc : contracts) {
            try {
                processed++;
                long daysLeft = ChronoUnit.DAYS.between(today, doc.getContractEndDate());

                // Already expired — nothing to alert
                if (daysLeft < 0) continue;

                boolean didAlert = false;

                // 7-day urgent threshold (fires first if both are unsent)
                if (daysLeft <= 7 && !Boolean.TRUE.equals(doc.getExpiryAlert7Sent())) {
                    sendAlerts(doc, daysLeft);
                    doc.setExpiryAlert7Sent(true);
                    didAlert = true;
                    logger.info("7-day alert sent for doc [{}] '{}'",
                        doc.getDocumentId(), doc.getTitle());
                }

                // 30-day early warning (skip if 7-day already fired on this run)
                if (daysLeft <= 30 && !Boolean.TRUE.equals(doc.getExpiryAlert30Sent())
                        && !didAlert) {
                    sendAlerts(doc, daysLeft);
                    doc.setExpiryAlert30Sent(true);
                    didAlert = true;
                    logger.info("30-day alert sent for doc [{}] '{}'",
                        doc.getDocumentId(), doc.getTitle());
                }

                if (didAlert) {
                    documentRepository.save(doc);
                    alerted++;
                }

            } catch (Exception e) {
                logger.error("Error processing doc [{}]: {}",
                    doc.getDocumentId(), e.getMessage());
            }
        }

        logger.info("ContractExpiryScheduler: done. Checked={}, Alerted={}", processed, alerted);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves recipients, then sends in-app notifications and emails to each.
     */
    private void sendAlerts(ImportableInformation doc, long daysLeft) {
        Set<Users> recipients = resolveRecipients(doc);

        // In-app — batch call (one per user, all in one transaction)
        try {
            notificationService.notifyContractExpiry(recipients, doc, daysLeft);
        } catch (Exception e) {
            logger.error("In-app notifications failed for doc [{}]: {}",
                doc.getDocumentId(), e.getMessage());
        }

        // Email — individual, each failure is isolated
        for (Users recipient : recipients) {
            try {
                String email = recipient.getEmail();
                if (email == null || email.isBlank()) {
                    logger.warn("No email for user '{}', skipping email for doc [{}]",
                        recipient.getUsername(), doc.getDocumentId());
                    continue;
                }
                emailService.sendContractExpiryAlert(
                    email,
                    recipient.getUsername(),
                    doc.getTitle(),
                    doc.getDocumentId(),
                    doc.getContractEndDate(),
                    daysLeft
                );
            } catch (Exception e) {
                logger.error("Email failed for user '{}' on doc [{}]: {}",
                    recipient.getUsername(), doc.getDocumentId(), e.getMessage());
            }
        }
    }

    /**
     * Determines the set of users to notify based on the document's viewer group.
     *
     * Always includes the uploader.
     * Never returns null — worst case returns just the uploader.
     */
    private Set<Users> resolveRecipients(ImportableInformation doc) {
        Set<Users> recipients = new LinkedHashSet<>();

        // Uploader is always notified
        if (doc.getUploadedBy() != null) {
            recipients.add(doc.getUploadedBy());
        }

        if (doc.getIntendedViewerGroup() == null) return recipients;

        int groupId = doc.getIntendedViewerGroup().getId();

        try {
            switch (groupId) {

                case VG_DEPARTMENT:
                    // All active users in the target department
                    if (doc.getTargetDepartment() != null) {
                        List<Users> deptUsers = usersRepository
                            .findActiveUsersByDepartmentId(doc.getTargetDepartment().getId());
                        recipients.addAll(deptUsers);
                        logger.debug("Doc [{}] — VG=Department, {} dept users resolved",
                            doc.getDocumentId(), deptUsers.size());
                    }
                    break;

                case VG_OFFICE:
                case VG_AREA:
                    // All active users in the target office
                    // (Area is intentionally scoped to office — area-wide blasts are too broad)
                    if (doc.getTargetOffice() != null) {
                        List<Users> officeUsers = usersRepository
                            .findActiveUsersByOfficeId(doc.getTargetOffice().getOfficeId());
                        recipients.addAll(officeUsers);
                        logger.debug("Doc [{}] — VG=Office/Area, {} office users resolved",
                            doc.getDocumentId(), officeUsers.size());
                    }
                    break;

                case VG_CUSTOM:
                    // All members of the custom viewer group
                    CustomViewerGroup cvg = doc.getCustomViewerGroup();
                    if (cvg != null && cvg.getMembers() != null) {
                        recipients.addAll(cvg.getMembers());
                        logger.debug("Doc [{}] — VG=Custom, {} group members resolved",
                            doc.getDocumentId(), cvg.getMembers().size());
                    }
                    break;

                case VG_ALL:
                default:
                    // Public/All — uploader only (already added above)
                    // Notifying every system user for a public contract is too noisy.
                    logger.debug("Doc [{}] — VG=All/Public, notifying uploader only",
                        doc.getDocumentId());
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to resolve recipients for doc [{}]: {}",
                doc.getDocumentId(), e.getMessage());
            // Fall back to uploader only — already in set
        }

        return recipients;
    }
}