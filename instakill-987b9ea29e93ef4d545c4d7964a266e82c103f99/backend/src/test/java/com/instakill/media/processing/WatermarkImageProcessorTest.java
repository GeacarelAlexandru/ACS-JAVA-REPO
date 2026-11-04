package com.instakill.media.processing;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WatermarkImageProcessorTest {

    private final WatermarkImageProcessor processor =
            new WatermarkImageProcessor(new ImageProcessingProperties(300, "Instakill Demo"));

    private static byte[] encode(int width, int height, int type, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static boolean hasNonBlackPixel(BufferedImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void scalesWideImageToMaxWidthKeepingAspectRatio() throws IOException {
        byte[] source = encode(1200, 800, BufferedImage.TYPE_INT_RGB, "png");

        BufferedImage result = decode(processor.process(source, "png"));

        assertThat(result.getWidth()).isEqualTo(300);
        assertThat(result.getHeight()).isEqualTo(200);
    }

    @Test
    void doesNotUpscaleSmallImages() throws IOException {
        byte[] source = encode(120, 80, BufferedImage.TYPE_INT_RGB, "png");

        BufferedImage result = decode(processor.process(source, "png"));

        assertThat(result.getWidth()).isEqualTo(120);
        assertThat(result.getHeight()).isEqualTo(80);
    }

    @Test
    void drawsWatermarkInBottomRightOnly() throws IOException {
        byte[] source = encode(600, 400, BufferedImage.TYPE_INT_RGB, "png");

        BufferedImage result = decode(processor.process(source, "png"));

        assertThat(hasNonBlackPixel(result, 140, 150, 300, 200)).as("bottom-right").isTrue();
        assertThat(hasNonBlackPixel(result, 0, 0, 140, 100)).as("top-left").isFalse();
    }

    @Test
    void producesJpegForJpgAndJpegExtensions() throws IOException {
        byte[] source = encode(500, 500, BufferedImage.TYPE_INT_RGB, "jpg");

        for (String ext : new String[]{"jpg", "jpeg"}) {
            byte[] out = processor.process(source, ext);
            // JPEG magic number
            assertThat(out[0] & 0xFF).isEqualTo(0xFF);
            assertThat(out[1] & 0xFF).isEqualTo(0xD8);
            assertThat(decode(out).getWidth()).isEqualTo(300);
        }
    }

    @Test
    void handlesGifAndPngWithAlpha() throws IOException {
        byte[] gif = encode(400, 400, BufferedImage.TYPE_INT_RGB, "gif");
        byte[] png = encode(400, 400, BufferedImage.TYPE_INT_ARGB, "png");

        assertThat(decode(processor.process(gif, "gif")).getWidth()).isEqualTo(300);
        assertThat(decode(processor.process(png, "png")).getWidth()).isEqualTo(300);
    }

    @Test
    void watermarkStillFitsOnTinyImages() throws IOException {
        byte[] source = encode(40, 20, BufferedImage.TYPE_INT_RGB, "png");

        BufferedImage result = decode(processor.process(source, "png"));

        assertThat(result.getWidth()).isEqualTo(40);
        assertThat(result.getHeight()).isEqualTo(20);
    }

    @Test
    void rejectsBytesThatAreNotAnImage() {
        assertThatThrownBy(() -> processor.process(new byte[]{1, 2, 3}, "png"))
                .isInstanceOf(ImageProcessingException.class)
                .hasMessageContaining("not a valid image");
    }

    @Test
    void supportsOnlyFormatsTheJdkCanHandle() {
        assertThat(processor.supports("PNG")).isTrue();
        assertThat(processor.supports("jpeg")).isTrue();
        assertThat(processor.supports("mp4")).isFalse();
        assertThat(processor.supports("webp")).isFalse();
        assertThat(processor.supports(null)).isFalse();
        assertThatThrownBy(() -> processor.process(new byte[]{1}, "mp4"))
                .isInstanceOf(ImageProcessingException.class);
    }
}
