package com.instakill.media.processing;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Scales images down to a maximum width (keeping the aspect ratio, never upscaling) and stamps a text watermark in the
 * bottom-right corner. Uses only the JDK's ImageIO/Java2D.
 *
 * <p>Limitations: animated GIFs keep only their first frame, and WebP is not supported by the JDK.
 */
@Component
public class WatermarkImageProcessor implements ImageProcessor {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final Set<String> SUPPORTED = Set.of("jpg", "jpeg", "png", "gif");

    private final int maxWidth;
    private final String watermarkText;

    public WatermarkImageProcessor(ImageProcessingProperties properties) {
        this.maxWidth = properties.maxWidth();
        this.watermarkText = properties.watermarkText();
    }

    @Override
    public boolean supports(String extension) {
        return extension != null && SUPPORTED.contains(extension.toLowerCase(Locale.ROOT));
    }

    @Override
    public byte[] process(byte[] source, String extension) {
        if (!supports(extension)) {
            throw new ImageProcessingException("Unsupported image format: " + extension);
        }
        String format = outputFormat(extension);
        BufferedImage original = read(source);

        BufferedImage result = scale(original, format);
        drawWatermark(result);
        return write(result, format);
    }

    private static String outputFormat(String extension) {
        String ext = extension.toLowerCase(Locale.ROOT);
        return ext.equals("jpeg") ? "jpg" : ext;
    }

    private static BufferedImage read(byte[] source) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
            if (image == null) {
                throw new ImageProcessingException("File is not a valid image");
            }
            return image;
        } catch (IOException e) {
            throw new ImageProcessingException("File is not a valid image", e);
        }
    }

    private BufferedImage scale(BufferedImage original, String format) {
        int width = Math.min(original.getWidth(), maxWidth);
        int height = Math.max(1, (int) Math.round(original.getHeight() * (width / (double) original.getWidth())));

        // JPEG has no alpha channel, so it needs an opaque canvas.
        int type = format.equals("jpg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage target = new BufferedImage(width, height, type);
        Graphics2D g = target.createGraphics();
        try {
            if (type == BufferedImage.TYPE_INT_RGB) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
            }
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // Area averaging gives much cleaner results than a single bicubic pass when shrinking a lot.
            Image scaled = width == original.getWidth()
                    ? original
                    : original.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
            g.drawImage(scaled, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private void drawWatermark(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver);

            int fontSize = Math.max(10, width / 18);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            FontMetrics metrics = g.getFontMetrics(font);
            int margin = Math.max(4, fontSize / 2);
            // Shrink the text for very small images so it always fits inside the picture.
            while (metrics.stringWidth(watermarkText) > width - 2 * margin && fontSize > 6) {
                fontSize--;
                font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
                metrics = g.getFontMetrics(font);
            }
            g.setFont(font);

            int x = Math.max(0, width - metrics.stringWidth(watermarkText) - margin);
            int y = Math.max(metrics.getAscent(), height - margin - metrics.getDescent());

            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(watermarkText, x + 1, y + 1);
            g.setColor(new Color(255, 255, 255, 230));
            g.drawString(watermarkText, x, y);
        } finally {
            g.dispose();
        }
    }

    private static byte[] write(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, out)) {
                throw new ImageProcessingException("Could not encode image as " + format);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new ImageProcessingException("Could not encode image as " + format, e);
        }
    }
}
