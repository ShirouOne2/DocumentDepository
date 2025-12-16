package com.docsdepository.demo.Controller.FileStorageController;

import com.docsdepository.demo.Entity.ImportableInformation;
import com.docsdepository.demo.Repository.ImportableInformationRepository;
import com.docsdepository.demo.Services.FileStorageService.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class FileManagerController {

    private static final Logger log =
            Logger.getLogger(FileManagerController.class.getName());

    private final FileStorageService fileStorageService;
    private final ImportableInformationRepository importableInformationRepository;

    @Autowired
    public FileManagerController(
            FileStorageService fileStorageService,
            ImportableInformationRepository importableInformationRepository) {

        this.fileStorageService = fileStorageService;
        this.importableInformationRepository = importableInformationRepository;
    }

    @PostMapping("/uploadfile")
    public boolean uploadfile(@RequestParam("file") MultipartFile file) {
        try {
            fileStorageService.saveFile(file);
            return true;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Exception during upload", e);
            return false;
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") Long documentId) {

        try {
            Optional<ImportableInformation> document =
                    importableInformationRepository.findById(documentId);

            if (document.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String filename = document.get().getFilename();
            var fileToDownload = fileStorageService.getDownloadFile(filename);

            return ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                    .contentLength(fileToDownload.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(
                            Files.newInputStream(fileToDownload.toPath())));

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error downloading file with ID: " + documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
