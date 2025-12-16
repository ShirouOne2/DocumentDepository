package com.docsdepository.demo.Controller.FileController;

import com.docsdepository.demo.Controller.DTO.UploadForm;
import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class FileController {

    
    private final ImportableInformationRepository fileRepository;

    public FileController(ImportableInformationRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @GetMapping("/files")
    public String viewMyFiles(Model model) {
        // Fetch ALL documents
        List<ImportableInformation> documents = fileRepository.findAll();
        
        // This log is CRUCIAL for debugging!
        System.out.println("DEBUG: Files Retrieved from DB: " + documents.size()); 
        model.addAttribute("activePage", "files");
        model.addAttribute("documents", documents); // The list name must be 'documents'
        model.addAttribute("uploadForm", new UploadForm());
        return "myfiles";
    }

    @GetMapping("/myfiles/search")
    public String searchFiles(@RequestParam("query") String query, Model model) {
        // Search by title (or integrate full-text search)
        List<ImportableInformation> searchResults = fileRepository.findByTitleContainingIgnoreCase(query);
        
        model.addAttribute("documents", searchResults);
        model.addAttribute("query", query); // To keep the search term in the input field
        
        return "myfiles";
    }

    // You will need to implement the @GetMapping("/delete/{id}") for the delete functionality
}