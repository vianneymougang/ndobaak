package com.ndobaak.backend.listing.repository;

import com.ndobaak.backend.listing.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByListingIdOrderByDisplayOrderAsc(Long listingId);

    int countByListingId(Long listingId);
}
