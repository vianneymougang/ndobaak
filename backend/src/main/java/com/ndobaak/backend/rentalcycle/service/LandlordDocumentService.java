package com.ndobaak.backend.rentalcycle.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.DocumentStatus;
import com.ndobaak.backend.common.enums.LandlordDocumentType;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.util.FileValidator;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.LandlordDocumentResponse;
import com.ndobaak.backend.rentalcycle.entity.LandlordDocument;
import com.ndobaak.backend.rentalcycle.mapper.RentalCycleMapper;
import com.ndobaak.backend.rentalcycle.repository.LandlordDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LandlordDocumentService {

    private final LandlordDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final NotificationDispatcher notificationDispatcher;
    private final RentalCycleMapper mapper;

    @Transactional
    public LandlordDocumentResponse uploadDocument(Long landlordId, MultipartFile file,
                                                   LandlordDocumentType type, Long authenticatedUserId) {
        if (!landlordId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Vous ne pouvez uploader que vos propres documents");
        }

        Landlord landlord = (Landlord) userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire non trouvé"));

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try {
            FileValidator.validateDocumentFile(file);
            String storageUrl = storageService.upload(
                    "landlord-documents",
                    originalFilename,
                    file.getInputStream(),
                    file.getSize(),
                    contentType
            );

            LandlordDocument document = new LandlordDocument();
            document.setLandlord(landlord);
            document.setType(type);
            document.setFileName(originalFilename);
            document.setStorageUrl(storageUrl);
            document.setStatus(DocumentStatus.PENDING);

            document = documentRepository.save(document);

            return mapper.toResponse(document);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'upload du document", e);
        }
    }

    @Transactional(readOnly = true)
    public List<LandlordDocumentResponse> getDocuments(Long landlordId, Long authenticatedUserId, boolean isAdmin) {
        if (!isAdmin && !landlordId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres documents");
        }

        return documentRepository.findByLandlordId(landlordId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public LandlordDocumentResponse validateDocument(Long docId, ValidationDecisionRequest decision, Long adminId) {
        LandlordDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé"));

        if (decision.status() == DocumentStatus.APPROVED) {
            document.setStatus(DocumentStatus.APPROVED);
            document.setValidatedById(adminId);
            document.setValidatedAt(LocalDateTime.now());
            document.setRejectionComment(null);
        } else if (decision.status() == DocumentStatus.REJECTED) {
            document.setStatus(DocumentStatus.REJECTED);
            document.setValidatedById(adminId);
            document.setValidatedAt(LocalDateTime.now());
            document.setRejectionComment(decision.comment());
        }

        document = documentRepository.save(document);

        NotificationType notificationType = decision.status() == DocumentStatus.APPROVED
                ? NotificationType.LANDLORD_DOCUMENT_APPROVED
                : NotificationType.LANDLORD_DOCUMENT_REJECTED;

        String title = decision.status() == DocumentStatus.APPROVED
                ? "Document approuvé"
                : "Document refusé";

        String body = decision.status() == DocumentStatus.APPROVED
                ? "Votre document \"" + document.getType().name() + "\" a été approuvé."
                : "Votre document \"" + document.getType().name() + "\" a été refusé." +
                  (decision.comment() != null && !decision.comment().isBlank()
                          ? " Motif : " + decision.comment() : "");

        notificationDispatcher.dispatch(
                document.getLandlord().getId(),
                notificationType,
                title,
                body,
                document.getId(),
                "LANDLORD_DOCUMENT",
                Map.<String, Object>of("documentType", document.getType().name()),
                Map.of()
        );

        return mapper.toResponse(document);
    }
}
