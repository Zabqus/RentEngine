package com.wzabek.rentengine.protocol.storage;

import com.wzabek.rentengine.common.exception.InvalidRequestException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

// Zdjęcia uszkodzeń na dysku (w Dockerze wolumen); w bazie trzymamy tylko ścieżkę względną
@Slf4j
@Component
public class PhotoStorage {

    private static final String FOLDER = "damages";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final Path root;

    public PhotoStorage(UploadProperties properties) {
        this.root = properties.dir().toAbsolutePath().normalize();
    }

    public String save(MultipartFile file) {
        if (file.isEmpty()) {
            throw invalidPhoto("must not be empty");
        }
        // Typ po zawartości pliku, bo nagłówek Content-Type ustawia klient
        String extension = detectExtension(file);
        String relative = FOLDER + "/" + UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Path target = root.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot store photo", e);
        }
        return relative;
    }

    public Resource load(String relative) {
        Path file = resolve(relative);
        if (!Files.isRegularFile(file)) {
            throw new NotFoundException("Photo file", relative);
        }
        return new FileSystemResource(file);
    }

    public void delete(String relative) {
        if (relative == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (IOException | NotFoundException e) {
            log.warn("Cannot delete photo {}: {}", relative, e.getMessage());
        }
    }

    public static MediaType mediaType(String relative) {
        return relative.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    // Ścieżka z bazy nie może wyjść poza katalog uploadów
    private Path resolve(String relative) {
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) {
            throw new NotFoundException("Photo file", relative);
        }
        return file;
    }

    private static String detectExtension(MultipartFile file) {
        byte[] header = new byte[PNG.length];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, header.length);
            if (startsWith(header, read, JPEG)) {
                return "jpg";
            }
            if (startsWith(header, read, PNG)) {
                return "png";
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read photo", e);
        }
        throw invalidPhoto("must be a JPEG or PNG image");
    }

    private static boolean startsWith(byte[] header, int read, byte[] signature) {
        return read >= signature.length && Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
    }

    private static InvalidRequestException invalidPhoto(String message) {
        return new InvalidRequestException(Map.of("photo", message));
    }
}
