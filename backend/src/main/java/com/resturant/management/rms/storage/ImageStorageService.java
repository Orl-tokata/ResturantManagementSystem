package com.resturant.management.rms.storage;

import com.resturant.management.rms.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

/**
 * Takes an uploaded picture and writes a safe, small copy of it to disk.
 *
 * <p>Nothing the caller sends is trusted: not the filename, not the declared
 * content type, not the extension. The bytes are read, checked, decoded, and
 * written out again as a new JPEG — so whatever arrives, what lands on disk is
 * an image this application produced.
 */
@Slf4j
@Service
@EnableConfigurationProperties(UploadProperties.class)
public class ImageStorageService {

    /** The formats {@link ImageIO} decodes without a plugin. */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final UploadProperties properties;
    private final Path root;

    public ImageStorageService(UploadProperties properties) throws IOException {
        this.properties = properties;
        this.root = Paths.get(properties.dir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Uploads directory: {}", root);
    }

    /**
     * Stores a picture and returns the name to keep on the row.
     *
     * @return a generated filename, never anything the caller chose
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.upload.empty");
        }
        long max = properties.maxImageSize().toBytes();
        if (file.getSize() > max) {
            throw new BadRequestException("error.upload.tooLarge",
                    properties.maxImageSize().toMegabytes());
        }

        byte[] bytes = read(file);
        requireImageBytes(bytes);

        BufferedImage source = decode(bytes);
        BufferedImage scaled = scaleToFit(source, properties.imageMaxEdge());

        String name = UUID.randomUUID() + ".jpg";
        try {
            // Re-encoded rather than copied. A file that decodes as an image
            // can still carry anything after the image data — an EXIF payload,
            // a zip appended to a JPEG. Writing the decoded pixels back out
            // leaves none of it, and strips location metadata from a phone
            // photograph as a side effect.
            if (!ImageIO.write(scaled, "jpg", root.resolve(name).toFile())) {
                throw new BadRequestException("error.upload.unreadable");
            }
        } catch (IOException e) {
            log.error("Could not write {}", name, e);
            throw new BadRequestException("error.upload.failed");
        }
        return name;
    }

    /** Removes a stored file. Quiet when it is already gone. */
    public void delete(String name) {
        if (name == null || name.isBlank()) return;
        try {
            Path path = resolve(name);
            if (path != null) Files.deleteIfExists(path);
        } catch (IOException e) {
            // A file left behind is untidy; a failed delete that broke the
            // surrounding operation would be worse.
            log.warn("Could not delete upload {}", name, e);
        }
    }

    /** Reads a stored file, or null when the name does not name one. */
    public byte[] load(String name) {
        Path path = resolve(name);
        if (path == null || !Files.isRegularFile(path)) return null;
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.warn("Could not read upload {}", name, e);
            return null;
        }
    }

    /**
     * Turns a stored name into a path inside the uploads directory, or null.
     *
     * <p>The name comes from a URL on the way back out, so it is caller input
     * again. Resolving and then checking the result is still under the root is
     * what stops {@code ../../application.yml} being served as a picture —
     * rejecting "/" and ".." by hand misses the encodings.
     */
    private Path resolve(String name) {
        if (name == null || name.isBlank()) return null;
        Path path = root.resolve(name).normalize();
        return path.startsWith(root) ? path : null;
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("error.upload.failed");
        }
    }

    /**
     * Checks the first bytes rather than the extension or the declared type.
     *
     * <p>Both of those are chosen by whoever is uploading. A file called
     * {@code lunch.jpg}, sent as {@code image/jpeg}, is still whatever its
     * bytes say it is.
     */
    private void requireImageBytes(byte[] bytes) {
        if (!startsWith(bytes, JPEG) && !startsWith(bytes, PNG)) {
            throw new BadRequestException("error.upload.notAnImage");
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }

    private BufferedImage decode(byte[] bytes) {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new BadRequestException("error.upload.notAnImage");
            return image;
        } catch (IOException e) {
            throw new BadRequestException("error.upload.notAnImage");
        }
    }

    /**
     * Scales down to fit a square of {@code maxEdge}, never up.
     *
     * <p>The POS draws tiles at about 128px. A phone photograph is several
     * thousand pixels across, and sending that to a till is bytes it downloads
     * and throws away on every load of the menu.
     */
    private BufferedImage scaleToFit(BufferedImage source, int maxEdge) {
        int w = source.getWidth();
        int h = source.getHeight();
        double factor = Math.min(1.0, (double) maxEdge / Math.max(w, h));

        int tw = Math.max(1, (int) Math.round(w * factor));
        int th = Math.max(1, (int) Math.round(h * factor));

        // TYPE_INT_RGB, not ARGB: the output is JPEG, which has no alpha
        // channel. Writing an image that has one produces the colours of a
        // photograph viewed through a broken filter.
        BufferedImage out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            // A PNG with transparency would otherwise come out on black.
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, tw, th);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, tw, th, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** Content type for everything this service stores. */
    public String contentType(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".jpg")
                ? "image/jpeg"
                : "application/octet-stream";
    }
}
