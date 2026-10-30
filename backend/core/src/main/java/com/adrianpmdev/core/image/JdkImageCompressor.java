package com.adrianpmdev.core.image;

import com.adrianpmdev.platform.image.CompressedImage;
import com.adrianpmdev.platform.image.ImageCompressor;
import com.adrianpmdev.platform.image.ImageSpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * {@link ImageCompressor} built only on the JDK (ImageIO and Java 2D), so it needs no native library and works the same
 * on any machine. Input: JPEG or PNG. Output: JPEG, or PNG when the picture has transparent pixels.
 * <p>
 * Memory is the limit on the free server, so pictures are checked before they are decoded (no more than
 * {@value #MAX_PIXELS} pixels) and only one picture is processed at a time. A large picture is never decoded at its
 * full size: the reader skips pixels so that it comes out at most {@value #DECODE_FACTOR} times the size it is stored
 * at, and the usual smooth scaling does the rest. A 40-megapixel photo then takes about 40 MB to process instead of
 * about 200 MB, which alone could stop the backend, and the stored picture looks the same.
 *
 * @author adrianpmdev
 */
@Component
public class JdkImageCompressor implements ImageCompressor {

    /** Largest picture accepted (40 megapixels), to bound the time spent reading it. */
    static final long MAX_PIXELS = 40_000_000L;

    /** A picture is decoded at most this many times the longest side it is stored at. */
    static final int DECODE_FACTOR = 2;

    private static final float[] JPEG_QUALITIES = {0.82f, 0.72f, 0.62f};
    private static final double SHRINK_STEP = 0.8;
    private static final int MAX_SHRINKS = 6;
    private static final int OPAQUE = 255;
    private static final long WAIT_SECONDS = 30;

    private final Semaphore permits = new Semaphore(1, true);

    /** Formats accepted as input, recognised by their first bytes. */
    private enum Format {
        JPEG, PNG;

        static Format detect(byte[] bytes) {
            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
                return JPEG;
            }

            if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
                return PNG;
            }

            return null;
        }
    }

    @Override
    public CompressedImage compress(byte[] source, ImageSpec spec) {
        if (source == null || source.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la imagen");
        }

        if (Format.detect(source) == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "La imagen debe ser JPEG o PNG");
        }

        acquire();

        try {
            BufferedImage image = withAlphaChannel(scaleToFit(decode(source, spec.maxSide()), spec.maxSide()));
            boolean transparent = hasTransparentPixels(image);

            return transparent ? encodePng(image, spec) : encodeJpeg(toRgb(image), spec);
        } finally {
            permits.release();
        }
    }

    /* ─── Decoding ─── */

    private static BufferedImage decode(byte[] source, int maxSide) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw unreadable();
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if ((long) width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                            "La imagen tiene demasiados píxeles (máximo 40 megapíxeles)");
                }

                ImageReadParam params = reader.getDefaultReadParam();
                int step = subsampling(Math.max(width, height), maxSide);
                params.setSourceSubsampling(step, step, 0, 0);

                return reader.read(0, params);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ResponseStatusException status) {
                throw status;
            }

            throw unreadable();
        }
    }

    /**
     * How many pixels the reader moves at a time so the picture comes out between {@code maxSide} and
     * {@value #DECODE_FACTOR} times {@code maxSide} on its longest side (1 keeps every pixel).
     */
    static int subsampling(int longest, int maxSide) {
        return Math.max(1, (int) Math.ceil((double) longest / ((long) DECODE_FACTOR * maxSide)));
    }

    private static ResponseStatusException unreadable() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "No se ha podido leer la imagen");
    }

    /* ─── Resizing ─── */

    private static BufferedImage scaleToFit(BufferedImage image, int maxSide) {
        int longest = Math.max(image.getWidth(), image.getHeight());

        return longest <= maxSide ? image : scale(image, (double) maxSide / longest);
    }

    /**
     * Scales a picture down. Large reductions are done in halves first, which keeps the result sharp without the
     * cost of a full bicubic pass on the original size.
     */
    private static BufferedImage scale(BufferedImage image, double factor) {
        int targetWidth = Math.max(1, (int) Math.round(image.getWidth() * factor));
        int targetHeight = Math.max(1, (int) Math.round(image.getHeight() * factor));
        BufferedImage current = image;

        while (current.getWidth() / 2 >= targetWidth && current.getHeight() / 2 >= targetHeight) {
            current = draw(current, current.getWidth() / 2, current.getHeight() / 2);
        }

        return current.getWidth() == targetWidth && current.getHeight() == targetHeight
                ? current
                : draw(current, targetWidth, targetHeight);
    }

    private static BufferedImage draw(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        return target;
    }

    /* ─── Encoding ─── */

    /**
     * Gives pictures that may be transparent a real alpha channel. Palette PNGs keep their transparency in the
     * palette, where the alpha raster cannot see it.
     */
    private static BufferedImage withAlphaChannel(BufferedImage image) {
        boolean needsCopy = image.getColorModel().hasAlpha() && image.getType() != BufferedImage.TYPE_INT_ARGB;

        return needsCopy ? draw(image, image.getWidth(), image.getHeight()) : image;
    }

    private static boolean hasTransparentPixels(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return false;
        }

        WritableRaster alpha = image.getAlphaRaster();

        if (alpha == null) {
            return false;
        }

        int[] row = new int[image.getWidth()];

        for (int y = 0; y < image.getHeight(); y++) {
            alpha.getPixels(0, y, image.getWidth(), 1, row);

            for (int value : row) {
                if (value < OPAQUE) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Flattens a picture without transparency onto white, the RGB layout JPEG needs. */
    private static BufferedImage toRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return rgb;
    }

    /** Lowers the quality first and then the size, until the file fits. */
    private static CompressedImage encodeJpeg(BufferedImage image, ImageSpec spec) {
        BufferedImage current = image;

        for (int shrinks = 0; shrinks <= MAX_SHRINKS; shrinks++) {
            for (float quality : JPEG_QUALITIES) {
                byte[] bytes = writeJpeg(current, quality);

                if (bytes.length <= spec.maxBytes()) {
                    return new CompressedImage(bytes, "image/jpeg", "jpg", current.getWidth(), current.getHeight());
                }
            }

            current = toRgb(scale(current, SHRINK_STEP));
        }

        throw tooHeavy();
    }

    /** PNG has no quality setting, so only the size can be lowered. */
    private static CompressedImage encodePng(BufferedImage image, ImageSpec spec) {
        BufferedImage current = image;

        for (int shrinks = 0; shrinks <= MAX_SHRINKS; shrinks++) {
            byte[] bytes = writePng(current);

            if (bytes.length <= spec.maxBytes()) {
                return new CompressedImage(bytes, "image/png", "png", current.getWidth(), current.getHeight());
            }

            current = scale(current, SHRINK_STEP);
        }

        throw tooHeavy();
    }

    private static byte[] writeJpeg(BufferedImage image, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), params);
        } catch (IOException exception) {
            throw new IllegalStateException("JPEG encoding failed", exception);
        } finally {
            writer.dispose();
        }

        return bytes.toByteArray();
    }

    private static byte[] writePng(BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("PNG encoding failed", exception);
        }

        return bytes.toByteArray();
    }

    private static ResponseStatusException tooHeavy() {
        return new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "No se ha podido comprimir la imagen lo suficiente");
    }

    private void acquire() {
        try {
            if (!permits.tryAcquire(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servidor está ocupado, inténtalo de nuevo");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servidor está ocupado, inténtalo de nuevo");
        }
    }
}
