package com.findsong.findsongapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@Slf4j
public class FileStorageUtil {

    private final Path uploadDir = Paths.get("uploads");

    public FileStorageUtil() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de uploads", e);
        }
    }

    public Path storeFile(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + ".audio";
        Path targetPath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath);
        return targetPath;
    }

    public void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error al eliminar archivo: {}", e.getMessage());
        }
    }
}
