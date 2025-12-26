package com.docsdepository.demo.Services.FileStorageService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String STORAGE_DIRECTORY;

    public String saveFile(MultipartFile fileToSave) throws IOException {
        if (fileToSave == null) {
            throw new NullPointerException("fileToSave is null");
        }

        File directory = new File(STORAGE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Extract original filename
        String originalName = fileToSave.getOriginalFilename();

        // Separate base name and extension
        String baseName = originalName;
        String extension = "";

        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex != -1) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        }

        // Create the target file path
        File targetFile = new File(directory, originalName);

        // 🔥 IF FILE ALREADY EXISTS → AUTO-RENAME
        int counter = 1;
        while (targetFile.exists()) {
            String newName = baseName + "(" + counter + ")" + extension;
            targetFile = new File(directory, newName);
            counter++;
        }

        // Write the file
        Files.copy(fileToSave.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Return final file name so Controller can save it to DB
        return targetFile.getName();
    }
    public File getDownloadFile(String fileName) throws Exception{
        if(fileName == null){
            throw new NullPointerException("filename is null");
        }
        var fileToDownload = new File(STORAGE_DIRECTORY + File.separator + fileName);
        if(!Objects.equals(fileToDownload.getParent(), STORAGE_DIRECTORY)){
            throw new SecurityException("Unsupported FileName: ");
        }
        if(!fileToDownload.exists()){
            throw new FileNotFoundException("No file named: "+ fileName);
        }
        return fileToDownload;
    }
}
