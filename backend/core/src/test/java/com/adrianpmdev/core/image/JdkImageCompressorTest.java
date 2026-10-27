package com.adrianpmdev.core.image;

import com.adrianpmdev.platform.image.CompressedImage;
import com.adrianpmdev.platform.image.ImageSpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Picture compression: formats, resizing, transparency, size limit and rejected files.
 *
 * @author adrianpmdev
 */
class JdkImageCompressorTest {

    private final JdkImageCompressor compressor = new JdkImageCompressor();

    @Test
    void largeOpaquePictureIsResizedToJpeg() throws IOException {
        byte[] png = encode(picture(3000, 2000, false), "png");

        CompressedImage result = compressor.compress(png, new ImageSpec(1600, 1024 * 1024));

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.extension()).isEqualTo("jpg");
        assertThat(result.width()).isEqualTo(1600);
        assertThat(result.height()).isEqualTo(1067);
        assertThat(result.bytes().length).isLessThanOrEqualTo(1024 * 1024);
        assertThat(decode(result.bytes()).getWidth()).isEqualTo(1600);
    }

    @Test
    void smallPictureKeepsItsSize() throws IOException {
        byte[] jpeg = encode(picture(400, 300, false), "jpg");

        CompressedImage result = compressor.compress(jpeg, new ImageSpec(1600, 1024 * 1024));

        assertThat(result.width()).isEqualTo(400);
        assertThat(result.height()).isEqualTo(300);
    }

    @Test
    void transparentPictureStaysPng() throws IOException {
        byte[] png = encode(picture(800, 600, true), "png");

        CompressedImage result = compressor.compress(png, new ImageSpec(1600, 1024 * 1024));

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(decode(result.bytes()).getColorModel().hasAlpha()).isTrue();
    }

    @Test
    void pictureIsCompressedUntilItFitsTheLimit() throws IOException {
        byte[] noisy = encode(noise(2000, 2000), "png");

        CompressedImage result = compressor.compress(noisy, new ImageSpec(2000, 150_000));

        assertThat(result.bytes().length).isLessThanOrEqualTo(150_000);
    }

    @Test
    void otherFilesAreRejected() {
        byte[] text = "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> compressor.compress(text, new ImageSpec(1600, 1024 * 1024)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void brokenPictureIsRejected() {
        byte[] broken = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};

        assertThatThrownBy(() -> compressor.compress(broken, new ImageSpec(1600, 1024 * 1024)))
                .isInstanceOf(ResponseStatusException.class);
    }

    /* ─── Helpers ─── */

    private static BufferedImage picture(int width, int height, boolean transparent) {
        BufferedImage image = new BufferedImage(width, height,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new GradientPaint(0, 0, Color.BLUE, width, height, Color.ORANGE));

        if (transparent) {
            graphics.fillOval(0, 0, width, height);
        } else {
            graphics.fillRect(0, 0, width, height);
        }

        graphics.dispose();

        return image;
    }

    private static BufferedImage noise(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }

        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);

        return bytes.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
