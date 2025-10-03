package ru.tishembitov.pictorium.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.tishembitov.pictorium.exception.FileStorageException;
import ru.tishembitov.pictorium.exception.InvalidFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final List<String> allowedExtensions;
    private final long maxFileSize;

    public FileStorageServiceImpl(
            @Value("${file.upload-dir}") String uploadDir,
            @Value("${file.allowed-extensions}") String allowedExts,
            @Value("${file.max-size}") long maxSize) {

        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedExtensions = Arrays.asList(allowedExts.split(","));
        this.maxFileSize = maxSize;

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory created at: {}", this.fileStorageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }


    public String saveFile(MultipartFile file, String subDirectory) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + "." + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetLocation);

            Path filePath = targetLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved successfully: {}", filePath);
            return subDirectory + "/" + newFilename;

        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }


    public void deleteFile(String filePath) {
        try {
            Path path = this.fileStorageLocation.resolve(filePath).normalize();
            Files.deleteIfExists(path);
            log.info("File deleted successfully: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new FileStorageException("Failed to delete file: " + filePath, e);
        }
    }

    public Path getFile(String filePath) {
        try {
            Path path = this.fileStorageLocation.resolve(filePath).normalize();
            if (!Files.exists(path)) {
                throw new FileStorageException("File not found: " + filePath);
            }
            return path;
        } catch (Exception e) {
            throw new FileStorageException("File not found: " + filePath, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidFileException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new InvalidFileException("Invalid file path: " + filename);
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new InvalidFileException("File type not allowed. Allowed types: " + String.join(", ", allowedExtensions));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new InvalidFileException("File has no extension");
        }
        return filename.substring(lastDotIndex + 1);
    }
}
