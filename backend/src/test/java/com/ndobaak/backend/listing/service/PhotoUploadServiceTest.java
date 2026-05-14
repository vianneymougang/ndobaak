package com.ndobaak.backend.listing.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.common.enums.PhotoStatus;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ForbiddenException;
import com.ndobaak.backend.listing.dto.PhotoUploadAck;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.entity.Photo;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.PhotoRepository;
import com.ndobaak.backend.media.service.ImageProcessingService;
import com.ndobaak.backend.media.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoUploadServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private StorageService storageService;
    @Mock private ImageProcessingService imageProcessingService;
    @Mock private AuthService authService;

    @InjectMocks
    private PhotoUploadService photoUploadService;

    @Test
    void scheduleUploadWithValidPngShouldReturnAck() throws IOException {
        Landlord landlord = buildLandlord(1L);
        Listing listing = Listing.builder().title("Test").landlord(landlord).build();
        listing.setId(10L);

        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngContent);

        when(listingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(photoRepository.countByListingId(10L)).thenReturn(2);

        Photo savedPhoto = Photo.builder().url("").displayOrder(2).status(PhotoStatus.PROCESSING).listing(listing).build();
        savedPhoto.setId(99L);
        when(photoRepository.save(any())).thenReturn(savedPhoto);

        PhotoUploadAck ack = photoUploadService.scheduleUpload(10L, file);

        assertThat(ack.photoId()).isEqualTo(99L);
        assertThat(ack.listingId()).isEqualTo(10L);
    }

    @Test
    void scheduleUploadWithPdfShouldThrow() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46};
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfContent);

        assertThatThrownBy(() -> photoUploadService.scheduleUpload(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Format de fichier non accepté");
    }

    @Test
    void scheduleUploadTooLargeShouldThrow() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        largeContent[0] = (byte) 0x89;
        largeContent[1] = 0x50;
        largeContent[2] = 0x4E;
        largeContent[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", largeContent);

        assertThatThrownBy(() -> photoUploadService.scheduleUpload(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("taille maximale");
    }

    @Test
    void scheduleUploadEmptyShouldThrow() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> photoUploadService.scheduleUpload(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void scheduleUploadFakeMimeShouldThrow() {
        byte[] exeContent = new byte[]{0x4D, 0x5A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "hack.jpg", "image/jpeg", exeContent);

        assertThatThrownBy(() -> photoUploadService.scheduleUpload(1L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Format de fichier non accepté");
    }

    @Test
    void scheduleUploadAtLimitShouldThrow() throws IOException {
        Landlord landlord = buildLandlord(1L);
        Listing listing = Listing.builder().title("Test").landlord(landlord).build();
        listing.setId(10L);

        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngContent);

        when(listingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(landlord);
        when(photoRepository.countByListingId(10L)).thenReturn(10);

        assertThatThrownBy(() -> photoUploadService.scheduleUpload(10L, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Maximum 10 photos");
    }

    @Test
    void scheduleUploadByNonOwnerShouldThrow() throws IOException {
        Landlord owner = buildLandlord(1L);
        Landlord other = buildLandlord(2L);
        Listing listing = Listing.builder().title("Test").landlord(owner).build();
        listing.setId(10L);

        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngContent);

        when(listingRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(listing));
        when(authService.getAuthenticatedUser()).thenReturn(other);

        assertThatThrownBy(() -> photoUploadService.scheduleUpload(10L, file))
                .isInstanceOf(ForbiddenException.class);
    }

    private Landlord buildLandlord(Long id) {
        Landlord landlord = Landlord.builder()
                .firstName("Test").lastName("User")
                .email("user" + id + "@test.com").phone("6900000" + id)
                .password("hash").build();
        landlord.setId(id);
        return landlord;
    }
}
