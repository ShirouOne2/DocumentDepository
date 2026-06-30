package com.docsdepository.demo.Services;

import com.docsdepository.demo.Entity.DocumentComment;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Notification;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.NotificationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ImportableInformationRepository documentRepository;

    @Autowired
    private PermissionService permissionService;

    // ─── Core ────────────────────────────────────────────────────────────────

    @Transactional
    public void createNotification(Users user, String type, String title,
                                   String message, String link) {
        try {
            Notification notification = new Notification(user, type, title, message, link);
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Failed to create notification for user {}: {}",
                user.getUsername(), e.getMessage());
        }
    }

    // ─── Document shared ─────────────────────────────────────────────────────

    @Transactional
    public void notifyDocumentShared(ImportableInformation document) {
        try {
            Users uploader = document.getUploadedBy();
            List<Users> allUsers = usersRepository.findAllWithDetails();

            int sent = 0;
            for (Users viewer : allUsers) {
                if (viewer.getUserId().equals(uploader.getUserId())) continue;
                if (canUserViewDocument(viewer, document)) {
                    createNotification(
                        viewer,
                        "document_shared",
                        "New Document Available",
                        uploader.getUsername() + " shared: " + document.getTitle(),
                        "/documents/details/" + document.getDocumentId()
                    );
                    sent++;
                }
            }
            logger.info("Sent {} notifications for document: {}", sent, document.getTitle());

        } catch (Exception e) {
            logger.error("Error sending notifications for document {}: {}",
                document.getDocumentId(), e.getMessage());
        }
    }

    private boolean canUserViewDocument(Users viewer, ImportableInformation document) {
        try {
            return permissionService.canUserAccessDocument(viewer, document);
        } catch (Exception e) {
            logger.error("Error checking view permission for user {}: {}",
                viewer.getUsername(), e.getMessage());
            return false;
        }
    }

    // ─── Comment ─────────────────────────────────────────────────────────────

    @Transactional
    public void notifyNewComment(DocumentComment comment) {
        try {
            Users owner = comment.getDocument().getUploadedBy();
            if (!owner.getUserId().equals(comment.getUser().getUserId())) {
                createNotification(
                    owner,
                    "new_comment",
                    "New Comment on Your Document",
                    comment.getUser().getUsername() + " commented on: "
                        + comment.getDocument().getTitle(),
                    "/documents/details/" + comment.getDocument().getDocumentId()
                );
            }
        } catch (Exception e) {
            logger.error("Error sending comment notification: {}", e.getMessage());
        }
    }

    // ─── Contract expiry ─────────────────────────────────────────────────────

    /**
     * Creates in-app contract-expiry notifications for every recipient
     * resolved by the scheduler.
     *
     * @param recipients   collection of users to notify (already de-duped by scheduler)
     * @param document     the contract document
     * @param daysLeft     days until contractEndDate
     */
    @Transactional
    public void notifyContractExpiry(Collection<Users> recipients,
                                     ImportableInformation document,
                                     long daysLeft) {

        String title = daysLeft <= 7
            ? "⚠ Contract Expiring Very Soon"
            : "Contract Expiring in " + daysLeft + " Days";

        String message = "\"" + document.getTitle() + "\" expires in "
            + daysLeft + " day" + (daysLeft == 1 ? "" : "s") + ". Please review.";

        int sent = 0;
        for (Users recipient : recipients) {
            try {
                createNotification(recipient, "contract_expiry", title, message, "/myfiles");
                sent++;
            } catch (Exception e) {
                logger.error("Failed to notify user {} for contract expiry on doc [{}]: {}",
                    recipient.getUsername(), document.getDocumentId(), e.getMessage());
            }
        }

        logger.info("Contract expiry in-app notifications sent to {}/{} recipients for doc [{}]",
            sent, recipients.size(), document.getDocumentId());
    }

    // ─── Read / query ─────────────────────────────────────────────────────────

    public Long getUnreadCount(Users user) {
        return notificationRepository
            .findByUserAndIsReadFalseOrderByCreatedAtDesc(user)
            .stream()
            .filter(n -> isNotificationValid(n, user))
            .count();
    }

    public List<Notification> getRecentNotifications(Users user) {
        return notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user)
            .stream()
            .filter(n -> isNotificationValid(n, user))
            .collect(Collectors.toList());
    }

    public List<Notification> getAllNotifications(Users user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .filter(n -> isNotificationValid(n, user))
            .collect(Collectors.toList());
    }

    private boolean isNotificationValid(Notification notif, Users user) {
        // Contract expiry notifications link to /myfiles — always keep them
        if ("contract_expiry".equals(notif.getType())) return true;

        if (notif.getLink() == null || !notif.getLink().contains("/documents/details/")) {
            return true;
        }

        try {
            String docIdStr = notif.getLink().substring(notif.getLink().lastIndexOf("/") + 1);
            Integer docId   = Integer.parseInt(docIdStr);
            ImportableInformation doc =
                documentRepository.findByIdWithDetails(docId).orElse(null);

            if (doc == null || Boolean.TRUE.equals(doc.getIsArchived())) return false;
            return permissionService.canUserAccessDocument(user, doc);

        } catch (NumberFormatException e) {
            logger.warn("Could not parse document ID from notification link: {}", notif.getLink());
            return true;
        } catch (Exception e) {
            logger.warn("Could not validate notification link: {}", notif.getLink());
            return true;
        }
    }

    @Transactional
    public void markAsRead(Integer notificationId, Users user) {
        notificationRepository.markAsRead(notificationId, user);
    }

    @Transactional
    public void markAllAsRead(Users user) {
        notificationRepository.markAllAsRead(user);
    }

    @Transactional
    public void cleanupOldNotifications(Users user) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldReadNotifications(user, cutoff);
    }
}