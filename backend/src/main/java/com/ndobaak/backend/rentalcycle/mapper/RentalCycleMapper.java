package com.ndobaak.backend.rentalcycle.mapper;

import com.ndobaak.backend.rentalcycle.dto.response.GuarantorResponse;
import com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse;
import com.ndobaak.backend.rentalcycle.dto.response.TenantDocumentResponse;
import com.ndobaak.backend.rentalcycle.entity.Guarantor;
import com.ndobaak.backend.rentalcycle.entity.TenantApplication;
import com.ndobaak.backend.rentalcycle.entity.TenantDocument;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RentalCycleMapper {

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
                document.getStorageUrl(),
                document.getCreatedAt()
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
