package com.ndobaak.backend.rentalcycle.service;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.ApplicationStatus;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.TenantDocumentType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.rentalcycle.dto.request.GuarantorRequest;
import com.ndobaak.backend.rentalcycle.dto.request.TenantApplicationRequest;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse;
import com.ndobaak.backend.rentalcycle.dto.response.TenantDocumentResponse;
import com.ndobaak.backend.rentalcycle.entity.Guarantor;
import com.ndobaak.backend.rentalcycle.entity.TenantApplication;
import com.ndobaak.backend.rentalcycle.entity.TenantDocument;
import com.ndobaak.backend.rentalcycle.mapper.RentalCycleMapper;
import com.ndobaak.backend.rentalcycle.repository.TenantApplicationRepository;
import com.ndobaak.backend.rentalcycle.repository.TenantDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TenantApplicationService {

    private final TenantApplicationRepository applicationRepository;
    private final TenantDocumentRepository documentRepository;
    private final BookingRepository bookingRepository;
    private final StorageService storageService;
    private final RentalCycleMapper mapper;

    @Transactional
    public TenantApplicationResponse createApplication(Long bookingId, TenantApplicationRequest request, Long studentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        if (!booking.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Accès non autorisé à cette réservation");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("La réservation doit être confirmée pour créer un dossier");
        }

        TenantApplication application = new TenantApplication();
        application.setBooking(booking);
        application.setStatus(ApplicationStatus.INCOMPLETE);

        for (GuarantorRequest guarantorReq : request.guarantors()) {
            Guarantor guarantor = new Guarantor();
            guarantor.setApplication(application);
            guarantor.setLastName(guarantorReq.lastName());
            guarantor.setFirstName(guarantorReq.firstName());
            guarantor.setIdCardNumber(guarantorReq.idCardNumber());
            guarantor.setPhone(guarantorReq.phone());
            guarantor.setAddress(guarantorReq.address());
            guarantor.setRelationshipToTenant(guarantorReq.relationshipToTenant());
            application.getGuarantors().add(guarantor);
        }

        application = applicationRepository.save(application);
        return mapper.toResponse(application);
    }

    @Transactional
    public TenantDocumentResponse uploadDocument(Long bookingId, MultipartFile file, TenantDocumentType type, Long studentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        if (!booking.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Accès non autorisé à cette réservation");
        }

        TenantApplication application = applicationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier locataire introuvable"));

        try {
            String storageKey = storageService.upload(
                    "tenant-documents",
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            TenantDocument document = new TenantDocument();
            document.setApplication(application);
            document.setType(type);
            document.setFileName(file.getOriginalFilename());
            document.setStorageUrl(storageKey);

            document = documentRepository.save(document);
            return mapper.toResponse(document);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'upload du document", e);
        }
    }

    @Transactional
    public TenantApplicationResponse submitApplication(Long bookingId, Long studentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        if (!booking.getStudent().getId().equals(studentId)) {
            throw new AccessDeniedException("Accès non autorisé à cette réservation");
        }

        TenantApplication application = applicationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier locataire introuvable"));

        boolean hasIdCard = application.getDocuments().stream()
                .anyMatch(doc -> doc.getType() == TenantDocumentType.ID_CARD);
        boolean hasStudentCard = application.getDocuments().stream()
                .anyMatch(doc -> doc.getType() == TenantDocumentType.STUDENT_CARD);

        if (!hasIdCard || !hasStudentCard) {
            throw new IllegalStateException("Documents requis manquants (CNI et carte étudiant)");
        }

        application.setStatus(ApplicationStatus.SUBMITTED);
        booking.setStatus(BookingStatus.APPLICATION_PENDING);

        applicationRepository.save(application);
        bookingRepository.save(booking);

        return mapper.toResponse(application);
    }

    @Transactional
    public TenantApplicationResponse validateApplication(Long bookingId, ValidationDecisionRequest decision, Long adminId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable"));

        TenantApplication application = applicationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier locataire introuvable"));

        application.setDecidedAt(LocalDateTime.now());
        application.setDecidedById(adminId);
        application.setDecisionComment(decision.comment());

        if (decision.status() == com.ndobaak.backend.common.enums.DocumentStatus.APPROVED) {
            application.setStatus(ApplicationStatus.APPROVED);
            booking.setStatus(BookingStatus.APPLICATION_APPROVED);
        } else {
            application.setStatus(ApplicationStatus.REJECTED);
            booking.setStatus(BookingStatus.APPLICATION_REJECTED);
        }

        applicationRepository.save(application);
        bookingRepository.save(booking);

        return mapper.toResponse(application);
    }
}
