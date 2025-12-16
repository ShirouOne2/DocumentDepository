package com.docsdepository.demo.Controller.DashboardController;

import com.docsdepository.demo.Repository.ImportableInformationRepository; // Optional DTO for Thymeleaf form

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class DashboardController {

    private final ImportableInformationRepository importableInformationRepository;

    @Autowired
    public DashboardController(ImportableInformationRepository importableInformationRepository) {
        this.importableInformationRepository = importableInformationRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model) {

        // 1️⃣ Total uploads overall
        long totalUploads = importableInformationRepository.count();

        // 2️⃣ Total uploads today
        LocalDate today = LocalDate.now();
        long uploadsToday = importableInformationRepository.countUploadsToday(today);

        // 3️⃣ Pass data to Thymeleaf
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "Dashboard - File Management");
        model.addAttribute("totalUploads", totalUploads);
        model.addAttribute("uploadsToday", uploadsToday);

        return "dashboard";
    }
}
