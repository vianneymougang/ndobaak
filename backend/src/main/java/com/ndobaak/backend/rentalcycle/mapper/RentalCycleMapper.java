package com.ndobaak.backend.rentalcycle.mapper;

import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.rentalcycle.dto.response.GuarantorResponse;
import com.ndobaak.backend.rentalcycle.dto.response.LandlordDocumentResponse;
import com.ndobaak.backend.rentalcycle.dto.response.LeaseContractResponse;
import com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse;
import com.ndobaak.backend.rentalcycle.dto.response.TenantDocumentResponse;
import com.ndobaak.backend.rentalcycle.entity.Guarantor;
import com.ndobaak.backend.rentalcycle.entity.LandlordDocument;
import com.ndobaak.backend.rentalcycle.entity.LeaseContract;
import com.ndobaak.backend.rentalcycle.entity.TenantApplication;
import com.ndobaak.backend.rentalcycle.entity.TenantDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RentalCycleMapper {

    private final StorageService storageService;

    public TenantApplicationResponse toResponse(TenantApplication application) {
        return new TenantApplicationResponse(
                application.getId(),
                application.getBooking().getId(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getDecidedAt(),
                application.getDecisionComment(),
                application.getDocuments().stream().map(this::toResponse).toList(),
                application.getGuarantors().stream().map(this::toResponse).toList()
        );
    }

    public TenantDocumentResponse toResponse(TenantDocument document) {
        return new TenantDocumentResponse(
                document.getId(),
                document.getType(),
                document.getFileName(),
                storageService.resolvePublicUrl(document.getStorageUrl()),
                document.getCreatedAt()
        );
    }

    public LandlordDocumentResponse toResponse(LandlordDocument document) {
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

    public LeaseContractResponse toResponse(LeaseContract lease) {
        return new LeaseContractResponse(
                lease.getId(),
                lease.getBooking().getId(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getMonthlyRent(),
                lease.getSecurityDeposit(),
                lease.getAgencyFee(),
                lease.getSpecialClauses(),
                lease.getPdfUrl() != null ? storageService.resolvePublicUrl(lease.getPdfUrl()) : null,
                lease.getStatus(),
                lease.getLandlordSignedAt(),
                lease.getTenantSignedAt(),
                lease.getTaxRegistrationDate(),
                lease.getTaxReference(),
                lease.getGeneratedById(),
                lease.getCreatedAt()
        );
    }

    public GuarantorResponse toResponse(Guarantor guarantor) {
        return new GuarantorResponse(
                guarantor.getId(),
                guarantor.getLastName(),
                guarantor.getFirstName(),
                guarantor.getIdCardNumber(),
                guarantor.getPhone(),
                guarantor.getAddress(),
                guarantor.getRelationshipToTenant()
        );
    }
}
