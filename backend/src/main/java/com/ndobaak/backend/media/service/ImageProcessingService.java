package com.ndobaak.backend.media.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class ImageProcessingService {

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int MEDIUM_WIDTH = 800;
    private static final int ORIGINAL_MAX_WIDTH = 1920;

    public record ResizedImages(
            byte[] original,
            byte[] medium,
            byte[] thumbnail
    ) {}

    public ResizedImages resize(InputStream inputStream) throws IOException {
        byte[] sourceBytes = inputStream.readAllBytes();

        byte[] original = resizeToMaxWidth(sourceBytes, ORIGINAL_MAX_WIDTH);
        byte[] medium = resizeToMaxWidth(sourceBytes, MEDIUM_WIDTH);
        byte[] thumbnail = resizeToMaxWidth(sourceBytes, THUMBNAIL_WIDTH);

        return new ResizedImages(original, medium, thumbnail);
    }

    private byte[] resizeToMaxWidth(byte[] source, int maxWidth) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(source))
                .width(maxWidth)
                .keepAspectRatio(true)
                .outputQuality(0.85)
                .toOutputStream(output);
        return output.toByteArray();
    }
}
