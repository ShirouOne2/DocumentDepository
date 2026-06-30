package com.docsdepository.demo.Controller.FileController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Repository.AreaRepository;
import com.docsdepository.demo.Repository.OfficeRepository;
import com.docsdepository.demo.Repository.DepartmentRepository;
import com.docsdepository.demo.Services.SearchAnalyticsService;
import com.docsdepository.demo.Services.CustomViewerGroupService;

import jakarta.servlet.http.HttpSession;

@Controller
public class FileController {

    @Autowired
    private ImportableInformationRepository fileRepository;
    
    @Autowired
    private DocumentClassificationRepository classificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SearchAnalyticsService searchAnalyticsService;
    
    @Autowired
    private CustomViewerGroupService customViewerGroupService;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private static final int PAGE_SIZE = 50;

    private boolean isAdmin(Users user) {
        return user != null && user.getRole() != null &&
               (user.getRole().getId() == 1 ||
                "ADMIN".equalsIgnoreCase(user.getRole().getName()));
    }

    @GetMapping("/myfiles")
    public String myFiles(
            @RequestParam(defaultValue = "0") int page,
            Model model, 
            HttpSession session) {
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }

        boolean isAdmin = isAdmin(currentUser);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("uploadDate").descending());
        Page<ImportableInformation> documentPage = 
            fileRepository.findByUploadedByAndIsArchivedFalse(currentUser, pageable);

        model.addAttribute("documents", documentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", documentPage.getTotalPages());
        model.addAttribute("totalItems", documentPage.getTotalElements());
        model.addAttribute("activePage", "myfiles");
        model.addAttribute("currentUserId", userId);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());

        if (isAdmin) {
            model.addAttribute("offices",     officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
        } else {
            model.addAttribute("offices",     java.util.Collections.emptyList());
            model.addAttribute("departments", java.util.Collections.emptyList());
        }

        return "myfiles";
    }
    
    @GetMapping("/myfiles/search")
    public String searchMyFiles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer classificationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/login";
        }

        boolean isAdmin = isAdmin(currentUser);

        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty()) 
            ? LocalDate.parse(startDate).atStartOfDay() 
            : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty()) 
            ? LocalDate.parse(endDate).atTime(LocalTime.MAX) 
            : null;

        // Parse search terms — split on comma, wrap each with % for LIKE contains match.
        // Splitting on comma (not whitespace) lets multi-word terms like "95-13" work.
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
        
        Page<ImportableInformation> documentPage = fileRepository.searchMyFilesMultiTerm(
                currentUser.getUserId(),
                term1, term2, term3,
                classificationId,
                startDateTime,
                endDateTime,
                pageable
        );

        // Record the search
        if (query != null && !query.trim().isEmpty()) {
            searchAnalyticsService.recordSearch(
                currentUser, 
                query, 
                "myfiles", 
                (int) documentPage.getTotalElements()
            );
        }

        model.addAttribute("documents", documentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", documentPage.getTotalPages());
        model.addAttribute("totalItems", documentPage.getTotalElements());
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("activePage", "myfiles");
        model.addAttribute("recentSearches", searchAnalyticsService.getRecentSearches(currentUser, 5));
        model.addAttribute("query", query);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("selectedClassification", classificationId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("customViewerGroups", customViewerGroupService.getAllActiveGroups());

        if (isAdmin) {
            model.addAttribute("offices",     officeRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
        } else {
            model.addAttribute("offices",     java.util.Collections.emptyList());
            model.addAttribute("departments", java.util.Collections.emptyList());
        }

        return "myfiles";
    }
    
    @GetMapping("/myfiles/export")
    public void exportToExcel(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer classificationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
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

        LocalDateTime startDateTime =
                (startDate != null && !startDate.isEmpty())
                        ? LocalDate.parse(startDate).atStartOfDay()
                        : null;

        LocalDateTime endDateTime =
                (endDate != null && !endDate.isEmpty())
                        ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                        : null;

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

        List<ImportableInformation> documents =
                fileRepository.searchMyFilesMultiTerm(
                        currentUser.getUserId(),
                        term1,
                        term2,
                        term3,
                        classificationId,
                        startDateTime,
                        endDateTime
                );

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        );

        String filename = String.format(
                "my-files_%s_%d-records.xlsx",
                timestamp,
                documents.size()
        );

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + filename + "\""
        );

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Documents");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Classification");
        header.createCell(2).setCellValue("Title");
        header.createCell(3).setCellValue("Description");
        header.createCell(4).setCellValue("Upload Date");
        header.createCell(5).setCellValue("Tags");

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
                    doc.getTags().stream()
                            .map(t -> t.getTagName())
                            .collect(Collectors.joining(", "))
            );
        }

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}