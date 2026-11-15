package residencia.gestao_condominio.services.images;

import residencia.gestao_condominio.services.exceptions.InvalidComplaintImageException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static residencia.gestao_condominio.services.images.ImageSanitizer.invalidImage;

final class WebpContainer {
    private static final int HEADER_SIZE = 12;
    private static final int CHUNK_HEADER_SIZE = 8;
    private static final int ANIMATION_FLAG = 0x02;
    private static final int ALPHA_FLAG = 0x10;

    private WebpContainer() {}

    // Keep only the still image's reconstruction data from the RIFF container (RFC 9649).
    static byte[] withoutMetadata(byte[] data) {
        var input = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (data.length < HEADER_SIZE || !fourCC(data, 0).equals("RIFF") || !fourCC(data, 8).equals("WEBP")
                || input.getInt(4) != data.length - CHUNK_HEADER_SIZE) {
            throw invalidImage();
        }

        var output = ByteBuffer.allocate(data.length).order(ByteOrder.LITTLE_ENDIAN);
        output.put(data, 0, HEADER_SIZE);
        int cursor = HEADER_SIZE;
        int width = 0;
        int height = 0;
        boolean alphaEnabled = false;
        boolean alphaChunk = false;
        boolean imageChunk = false;
        while (cursor < data.length) {
            if (data.length - cursor < CHUNK_HEADER_SIZE) {
                throw invalidImage();
            }
            String type = fourCC(data, cursor);
            int size = input.getInt(cursor + 4);
            int body = cursor + CHUNK_HEADER_SIZE;
            long end = body + (long) size + (size & 1);
            if (size < 0 || end > data.length) {
                throw invalidImage();
            }
            switch (type) {
                case "VP8X" -> {
                    if (cursor != HEADER_SIZE || size != 10) {
                        throw invalidImage();
                    }
                    if ((data[body] & ANIMATION_FLAG) != 0) {
                        throw animatedImage();
                    }
                    alphaEnabled = (data[body] & ALPHA_FLAG) != 0;
                    width = read24(data, body + 4) + 1;
                    height = read24(data, body + 7) + 1;
                    output.put(data, cursor, CHUNK_HEADER_SIZE);
                    // Clear all metadata flags and reserved bytes, keeping alpha and canvas dimensions.
                    output.putInt(alphaEnabled ? ALPHA_FLAG : 0);
                    output.put(data, body + 4, 6);
                }
                case "ALPH" -> {
                    if (!alphaEnabled || alphaChunk || imageChunk || size < 2) {
                        throw invalidImage();
                    }
                    int compression = data[body] & 3;
                    if (compression > 1 || (compression == 0 && size - 1 != (long) width * height)) {
                        throw invalidImage();
                    }
                    alphaChunk = true;
                    appendChunk(output, data, cursor, size);
                }
                case "VP8 ", "VP8L" -> {
                    if (imageChunk || (type.equals("VP8L") && alphaChunk)
                            || (type.equals("VP8 ") && alphaEnabled != alphaChunk)) {
                        throw invalidImage();
                    }
                    validateFrame(input, type, body, size, width, height);
                    imageChunk = true;
                    appendChunk(output, data, cursor, size);
                }
                case "ANIM", "ANMF" -> throw animatedImage();
                default -> {
                    // Drop EXIF, XMP, ICC profiles and any application-specific chunks.
                }
            }
            cursor = (int) end;
        }
        if (!imageChunk) {
            throw invalidImage();
        }
        output.putInt(4, output.position() - CHUNK_HEADER_SIZE);
        return Arrays.copyOf(output.array(), output.position());
    }

    private static void appendChunk(ByteBuffer output, byte[] data, int offset, int size) {
        output.put(data, offset, CHUNK_HEADER_SIZE + size);
        if ((size & 1) != 0) {
            output.put((byte) 0);
        }
    }

    private static void validateFrame(ByteBuffer input, String type, int offset, int size, int canvasWidth, int canvasHeight) {
        int width;
        int height;
        if (type.equals("VP8 ")) {
            byte[] data = input.array();
            if (size < 10 || (data[offset] & 1) != 0 || (data[offset + 3] & 0xff) != 0x9d
                    || data[offset + 4] != 0x01 || data[offset + 5] != 0x2a) {
                throw invalidImage();
            }
            width = input.getShort(offset + 6) & 0x3fff;
            height = input.getShort(offset + 8) & 0x3fff;
        } else {
            if (size < 5 || input.get(offset) != 0x2f) {
                throw invalidImage();
            }
            int header = input.getInt(offset + 1);
            if ((header >>> 29) != 0) {
                throw invalidImage();
            }
            width = (header & 0x3fff) + 1;
            height = ((header >>> 14) & 0x3fff) + 1;
        }
        if (width == 0 || height == 0 || (canvasWidth != 0 && (width != canvasWidth || height != canvasHeight))) {
            throw invalidImage();
        }
    }

    private static String fourCC(byte[] data, int offset) {
        return new String(data, offset, 4, StandardCharsets.US_ASCII);
    }

    private static int read24(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset]) | Byte.toUnsignedInt(data[offset + 1]) << 8
                | Byte.toUnsignedInt(data[offset + 2]) << 16;
    }

    private static InvalidComplaintImageException animatedImage() {
        return new InvalidComplaintImageException("Animated WebP images are not supported");
    }
}
