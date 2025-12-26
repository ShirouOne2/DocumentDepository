package com.docsdepository.demo.Controller.SharedFilesController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Entity.Users;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Repository.DocumentClassificationRepository;
import com.docsdepository.demo.Repository.UsersRepository;
import com.docsdepository.demo.Services.SearchAnalyticsService;

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

    public SharedFilesController(ImportableInformationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/shared")
    public String sharedFiles(Model model, HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        
        // Check if user is admin (role ID 4)
        boolean isAdmin = currentUser.getRole().getId() == 4;
        
        List<ImportableInformation> documents;
        
        if (isAdmin) {
            // Admin sees ALL non-archived documents
            documents = repository.findByIsArchivedFalse();
        } else {
            // Regular users see only documents shared with them
            Integer officeId = currentUser.getOffice().getOfficeId();
            Integer areaId = currentUser.getOffice().getArea().getId();
            Integer departmentId = currentUser.getOffice().getDepartment().getId();

            documents = repository.findSharedFiles(
                userId,
                officeId,
                areaId,
                departmentId
            );
        }

        model.addAttribute("documents", documents);
        model.addAttribute("activePage", "shared");
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("uploaders", usersRepository.findAll());
        model.addAttribute("isAdmin", isAdmin);

        return "shared-files";
    }
    
    @GetMapping("/shared/search")
    public String searchSharedFiles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer classificationId,
            @RequestParam(required = false) Integer uploadedById,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
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

        // Check if user is admin
        boolean isAdmin = currentUser.getRole().getId() == 4;

        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty())
                ? LocalDate.parse(startDate).atStartOfDay()
                : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty())
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                : null;

        List<ImportableInformation> documents;
        
        if (isAdmin) {
            // Admin searches ALL documents
            documents = repository.searchAllDocuments(
                    query,
                    classificationId,
                    uploadedById,
                    startDateTime,
                    endDateTime
            );
        } else {
            // Regular users search only their shared documents
            Integer officeId = currentUser.getOffice().getOfficeId();
            Integer areaId = currentUser.getOffice().getArea().getId();
            Integer departmentId = currentUser.getOffice().getDepartment().getId();

            documents = repository.searchSharedFiles(
                    userId,
                    officeId,
                    areaId,
                    departmentId,
                    query,
                    classificationId,
                    uploadedById,
                    startDateTime,
                    endDateTime
            );
        }

        // Record the search
        if (query != null && !query.trim().isEmpty()) {
            searchAnalyticsService.recordSearch(
                currentUser, 
                query, 
                "shared", 
                documents.size()
            );
        }

        model.addAttribute("documents", documents);
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("uploaders", usersRepository.findAll());
        model.addAttribute("activePage", "shared");
        model.addAttribute("isAdmin", isAdmin);
        
        // Add recent searches
        model.addAttribute("recentSearches", searchAnalyticsService.getRecentSearches(currentUser, 5));

        model.addAttribute("query", query);
        model.addAttribute("selectedClassification", classificationId);
        model.addAttribute("selectedUploader", uploadedById);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "shared-files";
    }
}