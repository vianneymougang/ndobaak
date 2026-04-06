package com.ndobaak.backend.rentalcycle.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.repository.UserRepository;
import com.ndobaak.backend.common.enums.DocumentStatus;
import com.ndobaak.backend.common.enums.LandlordDocumentType;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.rentalcycle.dto.request.ValidationDecisionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.LandlordDocumentResponse;
import com.ndobaak.backend.rentalcycle.entity.LandlordDocument;
import com.ndobaak.backend.rentalcycle.repository.LandlordDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LandlordDocumentService {

    private final LandlordDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional
    public LandlordDocumentResponse uploadDocument(Long landlordId, MultipartFile file, 
                                                   LandlordDocumentType type, Long authenticatedUserId) {
        if (!landlordId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Vous ne pouvez uploader que vos propres documents");
        }

        Landlord landlord = (Landlord) userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire non trouvé"));

        try {
            String storageUrl = storageService.upload(
                    "landlord-documents",
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            LandlordDocument document = new LandlordDocument();
            document.setLandlord(landlord);
            document.setType(type);
            document.setFileName(file.getOriginalFilename());
            document.setStorageUrl(storageUrl);
            document.setStatus(DocumentStatus.PENDING);

            document = documentRepository.save(document);

            return toResponse(document);
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
                .map(this::toResponse)
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

        // TODO Phase 5: Émettre notification LANDLORD_DOCUMENT_APPROVED / LANDLORD_DOCUMENT_REJECTED

        return toResponse(document);
    }

    private LandlordDocumentResponse toResponse(LandlordDocument document) {
        return new LandlordDocumentResponse(
                document.getId(),
                document.getLandlord().getId(),
                document.getType(),
                document.getFileName(),
                storageService.resolvePublicUrl(document.getStorageUrl()),
                document.getStatus(),
                document.getValidatedById(),
                document.getValidatedAt(),
                document.getRejectionComment(),
                document.getCreatedAt()
        );
    }
}
