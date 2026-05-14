package com.ndobaak.backend.common.util;

import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public final class FileValidator {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf"
    );

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    private FileValidator() {}

    public static void validateImageFile(MultipartFile file) throws IOException {
        validateSize(file);
        validateMimeType(file, IMAGE_MIME_TYPES, "Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP.");
        validateImageMagicBytes(file.getInputStream());
    }

    public static void validateDocumentFile(MultipartFile file) throws IOException {
        validateSize(file);
        validateMimeType(file, DOCUMENT_MIME_TYPES, "Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP, PDF.");
        validateDocumentMagicBytes(file.getInputStream());
    }

    private static void validateSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Le fichier est vide.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.UPLOAD_TOO_LARGE, "Le fichier dépasse la taille maximale de 10 Mo.");
        }
    }

    private static void validateMimeType(MultipartFile file, Set<String> allowed, String errorMessage) {
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, errorMessage);
        }
    }

    private static void validateImageMagicBytes(InputStream inputStream) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 4) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP.");
        }

        boolean isJpeg = bytesRead >= 3
                && header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
        boolean isPng = bytesRead >= 4
                && header[0] == PNG_MAGIC[0] && header[1] == PNG_MAGIC[1]
                && header[2] == PNG_MAGIC[2] && header[3] == PNG_MAGIC[3];
        boolean isWebp = bytesRead >= 12
                && header[0] == RIFF_MAGIC[0] && header[1] == RIFF_MAGIC[1]
                && header[2] == RIFF_MAGIC[2] && header[3] == RIFF_MAGIC[3]
                && header[8] == WEBP_MAGIC[0] && header[9] == WEBP_MAGIC[1]
                && header[10] == WEBP_MAGIC[2] && header[11] == WEBP_MAGIC[3];

        if (!isJpeg && !isPng && !isWebp) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP.");
        }
    }

    private static void validateDocumentMagicBytes(InputStream inputStream) throws IOException {
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 4) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP, PDF.");
        }

        boolean isJpeg = bytesRead >= 3
                && header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1] && header[2] == JPEG_MAGIC[2];
        boolean isPng = bytesRead >= 4
                && header[0] == PNG_MAGIC[0] && header[1] == PNG_MAGIC[1]
                && header[2] == PNG_MAGIC[2] && header[3] == PNG_MAGIC[3];
        boolean isWebp = bytesRead >= 12
                && header[0] == RIFF_MAGIC[0] && header[1] == RIFF_MAGIC[1]
                && header[2] == RIFF_MAGIC[2] && header[3] == RIFF_MAGIC[3]
                && header[8] == WEBP_MAGIC[0] && header[9] == WEBP_MAGIC[1]
                && header[10] == WEBP_MAGIC[2] && header[11] == WEBP_MAGIC[3];
        boolean isPdf = bytesRead >= 4
                && header[0] == PDF_MAGIC[0] && header[1] == PDF_MAGIC[1]
                && header[2] == PDF_MAGIC[2] && header[3] == PDF_MAGIC[3];

        if (!isJpeg && !isPng && !isWebp && !isPdf) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "Format de fichier non accepté. Formats autorisés : JPEG, PNG, WebP, PDF.");
        }
    }
}
