package com.docsdepository.demo.Controller.ArchivedFilesController;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.AreaRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.CustomViewerGroupService;
import com.docsdepository.demo.Services.SearchAnalyticsService;

@Controller
public class ArchivedFilesController {

    @Autowired
    private ImportableInformationRepository repository;
    
    @Autowired
    private DocumentClassificationRepository classificationRepository;
    
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SearchAnalyticsService searchAnalyticsService;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CustomViewerGroupService customViewerGroupService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDirectory;

    private static final int PAGE_SIZE = 50;

    /**
     * Check if file exists in storage
     */
    public boolean fileExists(ImportableInformation document) {
        if (document == null || document.getFilename() == null) {
            return false;
        }
        File file = new File(uploadDirectory + File.separator + document.getFilename());
        return file.exists() && file.isFile();
    }

    /**
     * API endpoint to check if document file exists
     */
    @GetMapping("/api/documents/{documentId}/file-exists")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkFileExists(
            @PathVariable Integer documentId,
            HttpSession session) {
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("exists", false));
        }

        try {
            ImportableInformation document = repository.findById(documentId)
                .orElse(null);
            
            if (document == null) {
                return ResponseEntity.ok(Map.of("exists", false));
            }
            
            boolean exists = fileExists(document);
            return ResponseEntity.ok(Map.of("exists", exists));
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }

    /**
     * Filter out versioned documents (but keep broken links for visibility)
     */
    private List<ImportableInformation> filterValidArchivedDocuments(List<ImportableInformation> documents) {
        return documents.stream()
            .filter(doc -> {
                // Exclude if this document was replaced by a newer version
                // (replacedByDocument != null means this is an old version)
                if (doc.getReplacedByDocument() != null) {
                    return false;
                }
                
                // Keep all other documents, including broken links
                return true;
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/archived")
    public String archivedFiles(
            @RequestParam(defaultValue = "0") int page,
            Model model, 
            HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        
        boolean isAdmin = currentUser.getRole() != null && 
                         (currentUser.getRole().getId() == 1 || 
                          "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));
        
        // Get all archived documents (we'll filter in Java)
        List<ImportableInformation> allArchivedDocs;
        
        if (isAdmin) {
            // Admin sees all archived documents
            allArchivedDocs = repository.findAll().stream()
                .filter(doc -> doc.getIsArchived() != null && doc.getIsArchived())
                .collect(Collectors.toList());
        } else {
            // Regular users see only archived documents they uploaded
            allArchivedDocs = repository.findByUploadedBy(currentUser).stream()
                .filter(doc -> doc.getIsArchived() != null && doc.getIsArchived())
                .collect(Collectors.toList());
        }

        // Filter out versioned documents (but keep broken links)
        List<ImportableInformation> validArchivedDocs = filterValidArchivedDocuments(allArchivedDocs);

        // Sort by archived date descending
        validArchivedDocs.sort((a, b) -> {
            LocalDateTime dateA = a.getArchivedDate() != null ? a.getArchivedDate() : a.getUploadDate();
            LocalDateTime dateB = b.getArchivedDate() != null ? b.getArchivedDate() : b.getUploadDate();
            return dateB.compareTo(dateA);
        });

        // Manual pagination
        int totalItems = validArchivedDocs.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalItems);
        
        List<ImportableInformation> pageContent = start < totalItems ? 
            validArchivedDocs.subList(start, end) : List.of();

        model.addAttribute("documents", pageContent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("activePage", "archived");
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("uploaders", usersRepository.findAll());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);
        
        if (isAdmin) {
            model.addAttribute("areas", areaRepository.findAll());
            model.addAttribute("offices", officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());
        }

        return "archived-files";
    }
    
    @GetMapping("/archived/search")
    public String searchArchivedFiles(    
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer classificationId,
            @RequestParam(required = false) Integer uploadedById,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer filterAreaId,
            @RequestParam(required = false) Integer filterOfficeId,
            @RequestParam(required = false) Integer filterDepartmentId,
            @RequestParam(required = false) Integer customGroupId,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }

        boolean isAdmin = currentUser.getRole() != null && 
                        (currentUser.getRole().getId() == 1 || 
                        "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty())
                ? LocalDate.parse(startDate).atStartOfDay()
                : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty())
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                : null;

        // Get all archived documents
        List<ImportableInformation> allArchivedDocs;
        
        if (isAdmin) {
            allArchivedDocs = repository.findAll().stream()
                .filter(doc -> doc.getIsArchived() != null && doc.getIsArchived())
                .collect(Collectors.toList());
        } else {
            allArchivedDocs = repository.findByUploadedBy(currentUser).stream()
                .filter(doc -> doc.getIsArchived() != null && doc.getIsArchived())
                .collect(Collectors.toList());
        }

        // Filter out versioned documents (but keep broken links)
        List<ImportableInformation> validArchivedDocs = filterValidArchivedDocuments(allArchivedDocs);

        // Apply search filters
        List<ImportableInformation> filteredDocs = validArchivedDocs.stream()
            .filter(doc -> {
                // Query filter (title, description, tags)
                if (query != null && !query.trim().isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    boolean matchesTitle = doc.getTitle() != null && doc.getTitle().toLowerCase().contains(lowerQuery);
                    boolean matchesDescription = doc.getDescription() != null && doc.getDescription().toLowerCase().contains(lowerQuery);
                    boolean matchesTags = doc.getTags() != null && doc.getTags().stream()
                        .anyMatch(tag -> tag.getTagName().toLowerCase().contains(lowerQuery));
                    
                    if (!matchesTitle && !matchesDescription && !matchesTags) {
                        return false;
                    }
                }

                // Classification filter
                if (classificationId != null && !doc.getDocumentClassification().getId().equals(classificationId)) {
                    return false;
                }

                // Uploader filter
                if (uploadedById != null && !doc.getUploadedBy().getUserId().equals(uploadedById)) {
                    return false;
                }

                // Date range filter (archived date)
                if (startDateTime != null && doc.getArchivedDate() != null && doc.getArchivedDate().isBefore(startDateTime)) {
                    return false;
                }
                if (endDateTime != null && doc.getArchivedDate() != null && doc.getArchivedDate().isAfter(endDateTime)) {
                    return false;
                }

                // Admin filters
                if (isAdmin) {
                    if (filterAreaId != null && doc.getUploadedBy().getOffice() != null && 
                        !doc.getUploadedBy().getOffice().getArea().getId().equals(filterAreaId)) {
                        return false;
                    }
                    if (filterOfficeId != null && doc.getUploadedBy().getOffice() != null &&
                        !doc.getUploadedBy().getOffice().getOfficeId().equals(filterOfficeId)) {
                        return false;
                    }
                    if (filterDepartmentId != null && doc.getUploadedBy().getDepartment() != null &&
                        !doc.getUploadedBy().getDepartment().getId().equals(filterDepartmentId)) {
                        return false;
                    }
                    if (customGroupId != null && doc.getCustomViewerGroup() != null &&
                        !doc.getCustomViewerGroup().getGroupId().equals(customGroupId)) {
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList());

        // Sort by archived date descending
        filteredDocs.sort((a, b) -> {
            LocalDateTime dateA = a.getArchivedDate() != null ? a.getArchivedDate() : a.getUploadDate();
            LocalDateTime dateB = b.getArchivedDate() != null ? b.getArchivedDate() : b.getUploadDate();
            return dateB.compareTo(dateA);
        });

        // Record search
        if (query != null && !query.trim().isEmpty()) {
            searchAnalyticsService.recordSearch(
                currentUser, 
                query, 
                "archived", 
                filteredDocs.size()
            );
        }

        // Manual pagination
        int totalItems = filteredDocs.size();
        int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalItems);
        
        List<ImportableInformation> pageContent = start < totalItems ? 
            filteredDocs.subList(start, end) : List.of();

        model.addAttribute("documents", pageContent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("uploaders", usersRepository.findAll());
        model.addAttribute("activePage", "archived");
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);
        
        if (isAdmin) {
            model.addAttribute("areas", areaRepository.findAll());
            model.addAttribute("offices", officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());
        }
        
        model.addAttribute("recentSearches", searchAnalyticsService.getRecentSearches(currentUser, 5));
        model.addAttribute("query", query);
        model.addAttribute("selectedClassification", classificationId);
        model.addAttribute("selectedUploader", uploadedById);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("filterAreaId", filterAreaId);
        model.addAttribute("filterOfficeId", filterOfficeId);
        model.addAttribute("filterDepartmentId", filterDepartmentId);
        model.addAttribute("customGroupId", customGroupId);

        return "archived-files";
    }

    /**
     * Restore an archived document
     */
    @PostMapping("/archived/restore/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreDocument(
            @PathVariable Integer documentId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "Not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            boolean isAdmin = currentUser.getRole() != null && 
                            (currentUser.getRole().getId() == 1 || 
                            "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

            // Get the document
            ImportableInformation document = repository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

            // Check permissions - only owner or admin can restore
            if (!isAdmin && !document.getUploadedBy().getUserId().equals(currentUser.getUserId())) {
                response.put("success", false);
                response.put("message", "You don't have permission to restore this document");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Check if document is actually archived
            if (document.getIsArchived() == null || !document.getIsArchived()) {
                response.put("success", false);
                response.put("message", "Document is not archived");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Check if this is a versioned document (old version)
            if (document.getReplacedByDocument() != null) {
                response.put("success", false);
                response.put("message", "Cannot restore old versions. This document has been replaced by a newer version.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Check if file exists
            if (!fileExists(document)) {
                response.put("success", false);
                response.put("message", "Cannot restore: File not found in storage. This is a broken link.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Restore the document
            document.setIsArchived(false);
            document.setArchivedDate(null);
            document.setArchivedBy(null);
            
            repository.save(document);

            response.put("success", true);
            response.put("message", "Document restored successfully");
            response.put("documentTitle", document.getTitle());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error restoring document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/archived/export")
    public void exportToExcel(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer classificationId,
            @RequestParam(required = false) Integer uploadedById,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer filterAreaId,
            @RequestParam(required = false) Integer filterOfficeId,
            @RequestParam(required = false) Integer filterDepartmentId,
            @RequestParam(required = false) Integer customGroupId,
            HttpServletResponse response,
            HttpSession session) throws IOException {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect("/login");
            return;
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            response.sendRedirect("/login");
            return;
        }

        boolean isAdmin = currentUser.getRole() != null && 
                        (currentUser.getRole().getId() == 1 || 
                        "ADMIN".equalsIgnoreCase(currentUser.getRole().getName()));

        // Get all archived documents with same filtering logic as search
        List<ImportableInformation> allArchivedDocs;
        
        if (isAdmin) {
            allArchivedDocs = repository.findAll().stream()
                .filter(doc -> doc.getIsArchived() != null && doc.getIsArchived())
                .collect(Collectors.toList());
        } else {
            allArchivedDocs = repository.findByUploadedBy(currentUser).stream()
                .filter(doc -> doc.getIsArchived() != null && doc.getIsArchived())
                .collect(Collectors.toList());
        }

        // Filter out versioned documents (but keep broken links)
        List<ImportableInformation> validArchivedDocs = filterValidArchivedDocuments(allArchivedDocs);

        // Apply search filters (same logic as search method)
        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty())
                ? LocalDate.parse(startDate).atStartOfDay()
                : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty())
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                : null;

        List<ImportableInformation> documents = validArchivedDocs.stream()
            .filter(doc -> {
                if (query != null && !query.trim().isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    boolean matches = (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(lowerQuery)) ||
                                    (doc.getDescription() != null && doc.getDescription().toLowerCase().contains(lowerQuery)) ||
                                    (doc.getTags() != null && doc.getTags().stream().anyMatch(tag -> tag.getTagName().toLowerCase().contains(lowerQuery)));
                    if (!matches) return false;
                }
                if (classificationId != null && !doc.getDocumentClassification().getId().equals(classificationId)) return false;
                if (uploadedById != null && !doc.getUploadedBy().getUserId().equals(uploadedById)) return false;
                if (startDateTime != null && doc.getArchivedDate() != null && doc.getArchivedDate().isBefore(startDateTime)) return false;
                if (endDateTime != null && doc.getArchivedDate() != null && doc.getArchivedDate().isAfter(endDateTime)) return false;
                
                if (isAdmin) {
                    if (filterAreaId != null && doc.getUploadedBy().getOffice() != null && 
                        !doc.getUploadedBy().getOffice().getArea().getId().equals(filterAreaId)) return false;
                    if (filterOfficeId != null && doc.getUploadedBy().getOffice() != null &&
                        !doc.getUploadedBy().getOffice().getOfficeId().equals(filterOfficeId)) return false;
                    if (filterDepartmentId != null && doc.getUploadedBy().getDepartment() != null &&
                        !doc.getUploadedBy().getDepartment().getId().equals(filterDepartmentId)) return false;
                    if (customGroupId != null && doc.getCustomViewerGroup() != null &&
                        !doc.getCustomViewerGroup().getGroupId().equals(customGroupId)) return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());

        // Generate filename
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = String.format(
                "archived-files_%s_%d-records.xlsx",
                timestamp,
                documents.size()
        );

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Archived Documents");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Classification");
        header.createCell(2).setCellValue("Title");
        header.createCell(3).setCellValue("Description");
        header.createCell(4).setCellValue("Upload Date");
        header.createCell(5).setCellValue("Archived Date");
        header.createCell(6).setCellValue("Uploaded By");
        header.createCell(7).setCellValue("Archived By");
        header.createCell(8).setCellValue("Tags");
        header.createCell(9).setCellValue("File Status");

        int rowIdx = 1;
        for (ImportableInformation doc : documents) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(doc.getDocumentId());
            row.createCell(1).setCellValue(
                    doc.getDocumentClassification() != null
                            ? doc.getDocumentClassification().getName()
                            : ""
            );
            row.createCell(2).setCellValue(doc.getTitle());
            row.createCell(3).setCellValue(doc.getDescription());
            row.createCell(4).setCellValue(
                    doc.getUploadDate() != null
                            ? doc.getUploadDate().toString()
                            : ""
            );
            row.createCell(5).setCellValue(
                    doc.getArchivedDate() != null
                            ? doc.getArchivedDate().toString()
                            : ""
            );
            row.createCell(6).setCellValue(
                    doc.getUploadedBy() != null
                            ? doc.getUploadedBy().getUsername()
                            : "System"
            );
            row.createCell(7).setCellValue(
                    doc.getArchivedBy() != null
                            ? doc.getArchivedBy().getUsername()
                            : "System"
            );
            row.createCell(8).setCellValue(
                    doc.getTags().stream()
                            .map(t -> t.getTagName())
                            .collect(Collectors.joining(", "))
            );
            row.createCell(9).setCellValue(
                    fileExists(doc) ? "OK" : "BROKEN LINK"
            );
        }

        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}