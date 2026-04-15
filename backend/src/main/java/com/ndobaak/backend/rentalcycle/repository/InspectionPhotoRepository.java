package com.ndobaak.backend.rentalcycle.repository;

import com.ndobaak.backend.rentalcycle.entity.InspectionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InspectionPhotoRepository extends JpaRepository<InspectionPhoto, Long> {
}
