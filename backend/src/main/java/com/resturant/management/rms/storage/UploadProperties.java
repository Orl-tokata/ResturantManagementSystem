package com.resturant.management.rms.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Where uploaded images live and how large they may be.
 *
 * @param dir          directory on disk; relative paths resolve against the
 *                     working directory
 * @param maxImageSize rejected above this, before the bytes are decoded
 * @param imageMaxEdge long edge a stored image is scaled down to
 */
@ConfigurationProperties(prefix = "app.uploads")
public record UploadProperties(String dir, DataSize maxImageSize, int imageMaxEdge) {
}
