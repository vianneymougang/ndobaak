package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.listing.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    int countByListingId(Long listingId);

    @Query("SELECT MAX(p.displayOrder) FROM Photo p WHERE p.listing.id = :listingId")
    Optional<Integer> findMaxDisplayOrderByListingId(Long listingId);
}
