package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.rentalcycle.entity.InspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionItemRepository extends JpaRepository<InspectionItem, Long> {
}
