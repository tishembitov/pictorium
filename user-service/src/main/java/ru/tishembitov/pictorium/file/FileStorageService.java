package ru.tishembitov.pictorium.file;

import java.nio.file.Path;

public interface FileStorageService {
    Path getFile(String filePath);
}
