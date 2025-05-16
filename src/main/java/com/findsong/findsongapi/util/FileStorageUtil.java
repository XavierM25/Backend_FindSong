package com.findsong.findsongapi.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FileStorageUtil {

    private final Path uploadDir;
    private final ScheduledExecutorService scheduler;

    @Value("${spring.servlet.multipart.location:${java.io.tmpdir}/findsong-uploads}")
    private String uploadPath;

    public FileStorageUtil() {
        this.uploadDir = Paths.get(System.getProperty("java.io.tmpdir"), "findsong-uploads");
        try {
            Files.createDirectories(uploadDir);
            log.info("Directorio de uploads creado en: {}", uploadDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de uploads", e);
        }

        // Configurar limpieza programada de archivos temporales
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::cleanupOldFiles, 1, 1, TimeUnit.HOURS);
    }

    public Path storeFile(MultipartFile file) throws IOException {
        String filename = generateUniqueFilename(file);
        Path targetPath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath);
        log.debug("Archivo guardado temporalmente: {}", targetPath);
        return targetPath;
    }

    public void deleteFile(Path filePath) {
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("Archivo eliminado: {}", filePath);
            } else {
                log.warn("No se pudo eliminar el archivo (no existe): {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error al eliminar archivo: {}", e.getMessage());
        }
    }

    private String generateUniqueFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return java.util.UUID.randomUUID() + extension;
    }

    private void cleanupOldFiles() {
        try {
            log.info("Iniciando limpieza de archivos temporales...");
            Instant now = Instant.now();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
                for (Path path : stream) {
                    try {
                        // Eliminar archivos con más de 1 hora de antigüedad
                        Instant fileTime = Files.getLastModifiedTime(path).toInstant();
                        if (Duration.between(fileTime, now).toHours() >= 1) {
                            deleteFile(path);
                        }
                    } catch (IOException e) {
                        log.warn("Error al procesar archivo durante limpieza: {}", path, e);
                    }
                }
            }
            log.info("Limpieza de archivos temporales completada");
        } catch (IOException e) {
            log.error("Error durante la limpieza de archivos temporales", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
