package com.docsdepository.demo.Controller.DashboardController; // Adjust package as necessary

import com.docsdepository.demo.Entity.UploadForm;
import com.docsdepository.demo.Repository.UploadFormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UploadFormRepository uploadFormRepository;
    
    // Inject the Repository to get statistics (like total count)
    @Autowired
    public DashboardController(UploadFormRepository uploadFormRepository) {
        this.uploadFormRepository = uploadFormRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Model model) {
        
        // ... (Your model attribute setup here) ...
        model.addAttribute("pageTitle", "Dashboard - File Management"); 
        model.addAttribute("uploadForm", new UploadForm()); 
        model.addAttribute("totalUploads", uploadFormRepository.count());
        
        // The return value "dashboard" tells Spring to render dashboard.html
        return "dashboard"; 
    }
    
    // You can add other dashboard-related GET mappings here later (e.g., /dashboard/reports)
}