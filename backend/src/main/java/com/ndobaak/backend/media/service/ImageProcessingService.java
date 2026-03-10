package com.ndobaak.backend.media.service;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageProcessingService {

    private static final int THUMBNAIL_SIZE = 300;
    private static final int MEDIUM_SIZE = 800;
    private static final int ORIGINAL_MAX_SIZE = 1920;
    private static final int WEBP_QUALITY = 85;

    private static final WebpWriter WRITER = new WebpWriter().withQ(WEBP_QUALITY).withMultiThread();

    public record ProcessedImages(
            byte[] original,
            byte[] medium,
            byte[] thumbnail
    ) {}

    public ProcessedImages process(InputStream inputStream) throws IOException {
        ImmutableImage source = ImmutableImage.loader().fromStream(inputStream);

        byte[] original = source.bound(ORIGINAL_MAX_SIZE, ORIGINAL_MAX_SIZE).bytes(WRITER);
        byte[] medium = source.bound(MEDIUM_SIZE, MEDIUM_SIZE).bytes(WRITER);
        byte[] thumbnail = source.bound(THUMBNAIL_SIZE, THUMBNAIL_SIZE).bytes(WRITER);

        return new ProcessedImages(original, medium, thumbnail);
    }
}
