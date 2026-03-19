package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.listing.dto.ListingResponse;
import com.ndobaak.backend.listing.entity.Listing;
import com.ndobaak.backend.listing.mapper.ListingMapper;
import com.ndobaak.backend.listing.repository.ListingRepository;
import com.ndobaak.backend.listing.repository.PhotoRepository;
import com.ndobaak.backend.notification.service.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminListingService {

    private static final Logger log = LoggerFactory.getLogger(AdminListingService.class);

    private final ListingRepository listingRepository;
    private final PhotoRepository photoRepository;
    private final ListingMapper listingMapper;
    private final NotificationDispatcher notificationDispatcher;

    public AdminListingService(ListingRepository listingRepository, PhotoRepository photoRepository,
                               ListingMapper listingMapper, NotificationDispatcher notificationDispatcher) {
        this.listingRepository = listingRepository;
        this.photoRepository = photoRepository;
        this.listingMapper = listingMapper;
        this.notificationDispatcher = notificationDispatcher;
    }

    public List<ListingResponse> getPendingListings() {
        return listingRepository.findByStatusOrderByCreatedAtDesc(ListingStatus.PENDING_REVIEW)
                .stream()
                .map(l -> listingMapper.toResponse(l, false))
                .toList();
    }

    @Transactional
    public ListingResponse validateListing(Long listingId) {
        Listing listing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw new BusinessException("Only listings with PENDING_REVIEW status can be validated");
        }

        int photoCount = photoRepository.countByListingId(listingId);
        if (photoCount < 3 || photoCount > 10) {
            throw new BusinessException("Listing must have between 3 and 10 photos");
        }

        listing.setStatus(ListingStatus.PUBLISHED);
        listing = listingRepository.save(listing);

        notificationDispatcher.dispatch(
                listing.getLandlord().getId(),
                NotificationType.LISTING_APPROVED,
                "Annonce approuvée",
                "Votre annonce \"" + listing.getTitle() + "\" a été approuvée et publiée.",
                listing.getId(),
                "LISTING",
                Map.of(
                        "landlordName", listing.getLandlord().getFirstName(),
                        "listingTitle", listing.getTitle()
                ),
                Map.of()
        );

        log.info("Listing {} validated and published by admin", listingId);

        return listingMapper.toResponse(listing, false);
    }

    @Transactional
    public ListingResponse rejectListing(Long listingId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Rejection reason is required");
        }

        Listing listing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw new BusinessException("Only listings with PENDING_REVIEW status can be rejected");
        }

        listing.setStatus(ListingStatus.REJECTED);
        listing.setRejectionReason(reason);
        listing = listingRepository.save(listing);

        notificationDispatcher.dispatch(
                listing.getLandlord().getId(),
                NotificationType.LISTING_REJECTED,
                "Annonce refusée",
                "Votre annonce \"" + listing.getTitle() + "\" a été refusée. Motif : " + reason,
                listing.getId(),
                "LISTING",
                Map.of(
                        "landlordName", listing.getLandlord().getFirstName(),
                        "listingTitle", listing.getTitle(),
                        "reason", reason
                ),
                Map.of()
        );

        log.info("Listing {} rejected by admin. Reason: {}", listingId, reason);

        return listingMapper.toResponse(listing, false);
    }
}
