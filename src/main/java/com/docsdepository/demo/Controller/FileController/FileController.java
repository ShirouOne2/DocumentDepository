package com.docsdepository.demo.Controller.FileController;

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
public class FileController {

    @Autowired
    private ImportableInformationRepository fileRepository;
    
    @Autowired
    private DocumentClassificationRepository classificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SearchAnalyticsService searchAnalyticsService;

    @GetMapping("/myfiles")
    public String myFiles(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/Userlogin";
        }

        List<ImportableInformation> documents = 
            fileRepository.findByUploadedByAndIsArchivedFalse(currentUser);

        model.addAttribute("documents", documents);
        model.addAttribute("activePage", "myfiles");
        model.addAttribute("classifications", classificationRepository.findAll());

        return "myfiles";
    }
    
    @GetMapping("/myfiles/search")
    public String searchMyFiles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer classificationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model,
            HttpSession session) {
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/Userlogin";
        }

        Users currentUser = usersRepository.findByIdWithOfficeHierarchy(userId);
        if (currentUser == null) {
            session.invalidate();
            return "redirect:/Userlogin";
        }

        LocalDateTime startDateTime = (startDate != null && !startDate.isEmpty()) 
            ? LocalDate.parse(startDate).atStartOfDay() 
            : null;

        LocalDateTime endDateTime = (endDate != null && !endDate.isEmpty()) 
            ? LocalDate.parse(endDate).atTime(LocalTime.MAX) 
            : null;

        List<ImportableInformation> documents = fileRepository.searchMyFiles(
                currentUser.getUserId(),
                query,
                classificationId,
                startDateTime,
                endDateTime
        );

        // Record the search
        if (query != null && !query.trim().isEmpty()) {
            searchAnalyticsService.recordSearch(
                currentUser, 
                query, 
                "myfiles", 
                documents.size()
            );
        }

        model.addAttribute("documents", documents);
        model.addAttribute("classifications", classificationRepository.findAll());
        model.addAttribute("activePage", "myfiles");
        
        // Add recent searches
        model.addAttribute("recentSearches", searchAnalyticsService.getRecentSearches(currentUser, 5));
        
        model.addAttribute("query", query);
        model.addAttribute("selectedClassification", classificationId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "myfiles";
    }
    
}