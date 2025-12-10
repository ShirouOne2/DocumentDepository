package com.docsdepository.demo.Services.FileStorageService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    public static final String STORAGE_DIRECTORY = "uploads";

    public void saveFile(MultipartFile fileToSave) throws IOException{
        if(fileToSave == null){
            throw new NullPointerException("fileToSave is null");
        }
        var targetFile = new File(STORAGE_DIRECTORY + File.separator  + fileToSave.getOriginalFilename());
        if(!Objects.equals(targetFile.getParent(), STORAGE_DIRECTORY)){
            throw new SecurityException("Unsupported FileName: ");
        }
        File directory = new File(STORAGE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs(); // Creates the directory if it doesn't exist
        }
        Files.copy(fileToSave.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
