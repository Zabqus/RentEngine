package com.wzabek.rentengine.protocol.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record DamagePhoto(Resource resource, MediaType mediaType) {
}
