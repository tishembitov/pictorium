package ru.tishembitov.pictorium.file;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    Path getFile(String filePath);

    void deleteFile(String image);

    String saveFile(MultipartFile file, String image);

}
