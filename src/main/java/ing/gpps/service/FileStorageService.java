package ing.gpps.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    @Value("${app.upload.path}")
    private String uploadPath;

    public String storeFile(MultipartFile file, String subDirectory) throws IOException {
        // Crear el directorio base si no existe
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Crear el subdirectorio si no existe
        Path subDir = uploadDir.resolve(subDirectory);
        if (!Files.exists(subDir)) {
            Files.createDirectories(subDir);
        }

        // Generar nombre único para el archivo
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = subDir.resolve(fileName);

        // Guardar el archivo
        Files.copy(file.getInputStream(), filePath);

        // Devolver la ruta relativa del archivo
        return subDirectory + "/" + fileName;
    }
} 