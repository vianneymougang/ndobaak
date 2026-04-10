package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.rentalcycle.entity.TenantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantDocumentRepository extends JpaRepository<TenantDocument, Long> {
}
