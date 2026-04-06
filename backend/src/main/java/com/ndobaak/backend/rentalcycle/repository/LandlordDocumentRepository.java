package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.common.enums.DocumentStatus;
import com.ndobaak.backend.rentalcycle.entity.LandlordDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandlordDocumentRepository extends JpaRepository<LandlordDocument, Long> {
    
    List<LandlordDocument> findByLandlordId(Long landlordId);
    
    List<LandlordDocument> findByStatus(DocumentStatus status);
}
