package org.app.UltraShort.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record URLRequest(
        @NotBlank(message = "URL cannot be empty")
        @Size(max = 2048, message = "URL is way too long!")
        String url) {
}
