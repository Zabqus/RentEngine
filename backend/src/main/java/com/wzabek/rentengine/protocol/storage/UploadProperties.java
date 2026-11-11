package com.wzabek.rentengine.protocol.storage;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties("app.uploads")
public record UploadProperties(
        @NotNull Path dir
) {
}
