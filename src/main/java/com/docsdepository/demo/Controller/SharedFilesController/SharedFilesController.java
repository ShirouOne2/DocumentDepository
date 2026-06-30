package com.docsdepository.demo.Controller.SharedFilesController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.AreaRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.SearchAnalyticsService;
import com.docsdepository.demo.Services.CustomViewerGroupService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpSession;

@Controller
public class SharedFilesController {

    private final ImportableInformationRepository repository;
    
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

    private static final int PAGE_SIZE = 50;

    public SharedFilesController(ImportableInformationRepository repository) {
        this.repository = repository;
    }

    // ─── /shared (initial page load) ─────────────────────────────────────

    @GetMapping("/shared")
    public String sharedFiles(
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

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("uploadDate").descending());

        Page<ImportableInformation> documentPage;

        if (isAdmin) {
            documentPage = repository.findByIsArchivedFalse(pageable);
        } else {
            Integer officeId    = currentUser.getOffice().getOfficeId();
            Integer areaId      = currentUser.getOffice().getArea().getId();
            Integer departmentId = currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null;

            documentPage = repository.findSharedFilesWithCustomGroups(
                    userId, officeId, areaId, departmentId, pageable);
        }

        model.addAttribute("documents",             documentPage.getContent());
        model.addAttribute("currentPage",           page);
        model.addAttribute("totalPages",            documentPage.getTotalPages());
        model.addAttribute("totalItems",            documentPage.getTotalElements());
        model.addAttribute("activePage",            "shared");
        model.addAttribute("classifications",       classificationRepository.findAll());
        model.addAttribute("uploaders",             usersRepository.findAll());
        model.addAttribute("isAdmin",               isAdmin);
        model.addAttribute("currentUserId",         userId);
        model.addAttribute("query",                 null);
        model.addAttribute("selectedClassification",null);
        model.addAttribute("selectedUploader",      null);
        model.addAttribute("startDate",             null);
        model.addAttribute("endDate",               null);
        model.addAttribute("filterAreaId",          null);
        model.addAttribute("filterOfficeId",        null);
        model.addAttribute("filterDepartmentId",    null);
        model.addAttribute("customGroupId",         null);

        if (isAdmin) {
            model.addAttribute("areas",       areaRepository.findAll());
            model.addAttribute("offices",     officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
        }
        model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());

        return "shared-files";
    }

    // ─── /shared/search ──────────────────────────────────────────────────

    @GetMapping("/shared/search")
    public String searchSharedFiles(
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
                ? LocalDate.parse(startDate).atStartOfDay() : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty())
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        // Parse search terms — split on comma, wrap each with % for LIKE contains match.
        String term1 = null, term2 = null, term3 = null;

        if (query != null && !query.trim().isEmpty()) {
            List<String> terms = Arrays.stream(query.trim().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(t -> !t.isEmpty())
                    .limit(3)
                    .collect(Collectors.toList());

            if (terms.size() > 0) term1 = "%" + terms.get(0) + "%";
            if (terms.size() > 1) term2 = "%" + terms.get(1) + "%";
            if (terms.size() > 2) term3 = "%" + terms.get(2) + "%";
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("uploadDate").descending());
        Page<ImportableInformation> documentPage;

        if (isAdmin) {
            documentPage = repository.searchAllDocumentsMultiTermWithFilters(
                    term1, term2, term3,
                    classificationId, uploadedById,
                    startDateTime, endDateTime,
                    filterAreaId, filterOfficeId, filterDepartmentId,
                    customGroupId,
                    pageable
            );
        } else {
            Integer officeId     = currentUser.getOffice().getOfficeId();
            Integer areaId       = currentUser.getOffice().getArea().getId();
            Integer departmentId = currentUser.getDepartment() != null ? currentUser.getDepartment().getId() : null;

            documentPage = repository.searchSharedFilesWithCustomGroupsMultiTermAndFilter(
                    userId, officeId, areaId, departmentId,
                    term1, term2, term3,
                    classificationId, uploadedById,
                    startDateTime, endDateTime,
                    null, null, null,
                    customGroupId,
                    pageable
            );
        }

        if (query != null && !query.trim().isEmpty()) {
            searchAnalyticsService.recordSearch(
                    currentUser, query, "shared", (int) documentPage.getTotalElements());
        }

        model.addAttribute("documents",              documentPage.getContent());
        model.addAttribute("totalItems",             documentPage.getTotalElements());
        model.addAttribute("currentPage",            page);
        model.addAttribute("totalPages",             documentPage.getTotalPages());
        model.addAttribute("classifications",        classificationRepository.findAll());
        model.addAttribute("uploaders",              usersRepository.findAll());
        model.addAttribute("activePage",             "shared");
        model.addAttribute("isAdmin",                isAdmin);
        model.addAttribute("currentUserId",          userId);
        model.addAttribute("query",                  query);
        model.addAttribute("selectedClassification", classificationId);
        model.addAttribute("selectedUploader",       uploadedById);
        model.addAttribute("startDate",              startDate);
        model.addAttribute("endDate",                endDate);
        model.addAttribute("filterAreaId",           filterAreaId);
        model.addAttribute("filterOfficeId",         filterOfficeId);
        model.addAttribute("filterDepartmentId",     filterDepartmentId);
        model.addAttribute("customGroupId",          customGroupId);

        if (isAdmin) {
            model.addAttribute("areas",       areaRepository.findAll());
            model.addAttribute("offices",     officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
        }
        model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());

        return "shared-files";
    }

    // ─── /shared/export ──────────────────────────────────────────────────

    @GetMapping("/shared/export")
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

        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty())
                ? LocalDate.parse(startDate).atStartOfDay() : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty())
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        // Parse search terms — split on comma, wrap each with % for LIKE contains match.
        String term1 = null, term2 = null, term3 = null;

        if (query != null && !query.trim().isEmpty()) {
            List<String> terms = Arrays.stream(query.trim().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(t -> !t.isEmpty())
                    .limit(3)
                    .toList();

            if (terms.size() > 0) term1 = "%" + terms.get(0) + "%";
            if (terms.size() > 1) term2 = "%" + terms.get(1) + "%";
            if (terms.size() > 2) term3 = "%" + terms.get(2) + "%";
        }

        List<ImportableInformation> documents;

        if (isAdmin) {
            documents = repository.searchAllDocumentsMultiTermWithFilters(
                    term1, term2, term3,
                    classificationId, uploadedById,
                    startDateTime, endDateTime,
                    filterAreaId, filterOfficeId, filterDepartmentId,
                    customGroupId,
                    Pageable.unpaged()
            ).getContent();
        } else {
            Integer officeId     = currentUser.getOffice().getOfficeId();
            Integer areaId       = currentUser.getOffice().getArea().getId();
            Integer departmentId = currentUser.getDepartment() != null ?
                                   currentUser.getDepartment().getId() : null;

            documents = repository.searchSharedFilesWithCustomGroupsMultiTermAndFilter(
                    userId, officeId, areaId, departmentId,
                    term1, term2, term3,
                    classificationId, uploadedById,
                    startDateTime, endDateTime,
                    filterAreaId, filterOfficeId, filterDepartmentId,
                    customGroupId,
                    Pageable.unpaged()
            ).getContent();
        }

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename  = String.format("shared-files_%s_%d-records.xlsx", timestamp, documents.size());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Shared Documents");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("Classification");
        headerRow.createCell(2).setCellValue("Title");
        headerRow.createCell(3).setCellValue("Description");
        headerRow.createCell(4).setCellValue("Upload Date");
        headerRow.createCell(5).setCellValue("Uploaded By");
        headerRow.createCell(6).setCellValue("Tags");

        int rowIdx = 1;
        for (ImportableInformation doc : documents) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(doc.getDocumentId());
            row.createCell(1).setCellValue(doc.getDocumentClassification() != null
                    ? doc.getDocumentClassification().getName() : "");
            row.createCell(2).setCellValue(doc.getTitle());
            row.createCell(3).setCellValue(doc.getDescription());
            row.createCell(4).setCellValue(doc.getUploadDate() != null
                    ? doc.getUploadDate().toString() : "");
            row.createCell(5).setCellValue(doc.getUploadedBy() != null
                    ? doc.getUploadedBy().getUsername() : "System");
            row.createCell(6).setCellValue(doc.getTags().stream()
                    .map(t -> t.getTagName())
                    .collect(Collectors.joining(", ")));
        }

        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}