package residencia.gestao_condominio.services.images;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import residencia.gestao_condominio.services.exceptions.InvalidComplaintImageException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.CRC32;

@Component
public class ImageSanitizer {
    private static final int MAX_STORED_BYTES = 5 * 1024 * 1024;
    private static final int MAX_PIXELS = 16_000_000;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    private static final int PNG_END = 0x49454e44;

    public record SanitizedImage(String mimeType, byte[] bytes) {}

    public SanitizedImage sanitize(String declaredContentType, byte[] data) {
        Format format = Format.fromContentType(declaredContentType);
        if (data.length == 0) {
            throw invalidImage();
        }

        byte[] sanitized;
        if (format == Format.WEBP) {
            sanitized = WebpContainer.withoutMetadata(data);
            decode(format, sanitized);
        } else {
            if (format == Format.PNG) {
                validatePngChunks(data);
            } else if (data.length < 4 || data[data.length - 2] != (byte) 0xff
                    || data[data.length - 1] != (byte) 0xd9) {
                throw invalidImage();
            }
            sanitized = encodePixels(format, decode(format, data));
        }

        if (sanitized.length > MAX_STORED_BYTES) {
            throw new InvalidComplaintImageException("Sanitized image exceeds 5 MiB");
        }
        return new SanitizedImage(format.mimeType, sanitized);
    }

    private static BufferedImage decode(Format format, byte[] data) {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }

            var reader = readers.next();
            try {
                if (!reader.getFormatName().equalsIgnoreCase(format.imageIoName)) {
                    throw invalidImage();
                }
                reader.setInput(input, true, true);
                reader.addIIOReadWarningListener((source, warning) -> {
                    // The container already validates the length of uncompressed alpha samples.
                    if (format != Format.WEBP || !"Uncompressed WebP alpha not implemented".equals(warning)) {
                        throw invalidImage();
                    }
                });
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new InvalidComplaintImageException("Image exceeds 16 megapixels");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (InvalidComplaintImageException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            // Decoders may throw unchecked exceptions on malformed image data.
            throw invalidImage();
        }
    }

    private static byte[] encodePixels(Format format, BufferedImage image) {
        int rasterType = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        var pixels = new BufferedImage(image.getWidth(), image.getHeight(), rasterType);
        var graphics = pixels.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        var buffer = new ByteArrayOutputStream();
        try (var output = new MemoryCacheImageOutputStream(buffer)) {
            if (!ImageIO.write(pixels, format.imageIoName, output)) {
                throw new IllegalStateException("Image writer unavailable for " + format.mimeType);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not encode sanitized image", ex);
        }
        return buffer.toByteArray();
    }

    private static void validatePngChunks(byte[] data) {
        if (data.length < PNG_SIGNATURE.length || !Arrays.equals(
                data, 0, PNG_SIGNATURE.length, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)) {
            throw invalidImage();
        }

        var buffer = ByteBuffer.wrap(data);
        buffer.position(PNG_SIGNATURE.length);
        var crc = new CRC32();
        while (buffer.remaining() >= 12) {
            int size = buffer.getInt();
            if (size < 0 || size > buffer.remaining() - 8) {
                throw invalidImage();
            }

            crc.reset();
            crc.update(data, buffer.position(), size + 4);
            int type = buffer.getInt();
            buffer.position(buffer.position() + size);
            if ((int) crc.getValue() != buffer.getInt()) {
                throw invalidImage();
            }
            // A PNG reader can finish decoding before seeing the end-of-image chunk.
            if (type == PNG_END) {
                if (size == 0 && !buffer.hasRemaining()) {
                    return;
                }
                throw invalidImage();
            }
        }
        throw invalidImage();
    }

    static InvalidComplaintImageException invalidImage() {
        return new InvalidComplaintImageException("Invalid image or image content does not match its declared MIME type");
    }

    private enum Format {
        JPEG("image/jpeg", "JPEG"),
        PNG("image/png", "PNG"),
        WEBP("image/webp", "WebP");

        private final String mimeType;
        private final String imageIoName;

        Format(String mimeType, String imageIoName) {
            this.mimeType = mimeType;
            this.imageIoName = imageIoName;
        }

        private static Format fromContentType(String contentType) {
            if (contentType == null) {
                throw new InvalidComplaintImageException("Image MIME type is required");
            }
            try {
                var mediaType = MediaType.parseMediaType(contentType);
                String mimeType = (mediaType.getType() + "/" + mediaType.getSubtype()).toLowerCase(Locale.ROOT);
                return switch (mimeType) {
                    case "image/jpeg", "image/jpg" -> JPEG;
                    case "image/png" -> PNG;
                    case "image/webp" -> WEBP;
                    default -> throw new InvalidComplaintImageException("Unsupported image MIME type");
                };
            } catch (IllegalArgumentException ex) {
                throw new InvalidComplaintImageException("Invalid image MIME type");
            }
        }
    }
}
