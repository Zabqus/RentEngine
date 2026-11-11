package com.wzabek.rentengine.protocol.storage;

import com.wzabek.rentengine.common.exception.InvalidRequestException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoStorageTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, 5};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    @TempDir
    Path dir;

    private PhotoStorage storage;

    @BeforeEach
    void setUp() {
        storage = new PhotoStorage(new UploadProperties(dir));
    }

    @Test
    void savesJpegAndLoadsItBack() throws Exception {
        String path = storage.save(new MockMultipartFile("photo", "a.jpg", "image/jpeg", JPEG));

        assertThat(path).startsWith("damages/").endsWith(".jpg");
        assertThat(storage.load(path).getContentAsByteArray()).isEqualTo(JPEG);
        assertThat(PhotoStorage.mediaType(path)).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void detectsPngByContentNotByDeclaredType() {
        String path = storage.save(new MockMultipartFile("photo", "a.jpg", "image/jpeg", PNG));

        assertThat(path).endsWith(".png");
        assertThat(PhotoStorage.mediaType(path)).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void rejectsFileThatIsNotAnImage() {
        MockMultipartFile fake = new MockMultipartFile("photo", "evil.jpg", "image/jpeg", "<script>".getBytes());

        assertThatThrownBy(() -> storage.save(fake))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errors").asString().contains("JPEG or PNG");
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> storage.save(new MockMultipartFile("photo", new byte[0])))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteRemovesFile() {
        String path = storage.save(new MockMultipartFile("photo", JPEG));

        storage.delete(path);

        assertThat(Files.exists(dir.resolve(path))).isFalse();
    }

    @Test
    void pathOutsideUploadDirIsNotServed() {
        assertThatThrownBy(() -> storage.load("../secret.txt")).isInstanceOf(NotFoundException.class);
    }
}
