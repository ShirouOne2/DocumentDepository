package com.docsdepository.demo.Controller.FileStorageController;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Services.FileStorageService.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.Optional;

@Controller
public class FileDeleteController {

    private final ImportableInformationRepository importableInformationRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public FileDeleteController(
            ImportableInformationRepository importableInformationRepository,
            FileStorageService fileStorageService) {

        this.importableInformationRepository = importableInformationRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Optional<ImportableInformation> document =
                importableInformationRepository.findById(id);

        if (document.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Document not found.");
            redirectAttributes.addFlashAttribute("status", "error");
            return "redirect:/files";
        }

        ImportableInformation fileRecord = document.get();
        String filename = fileRecord.getFilename();

        try {
            File fileToDelete = fileStorageService.getDownloadFile(filename);
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to delete file from storage.");
            redirectAttributes.addFlashAttribute("status", "error");
            return "redirect:/files";
        }

        // Delete DB record AFTER file deletion
        importableInformationRepository.deleteById(id);

        redirectAttributes.addFlashAttribute("message", "File deleted successfully!");
        redirectAttributes.addFlashAttribute("status", "success");

        return "redirect:/files";
    }
}
