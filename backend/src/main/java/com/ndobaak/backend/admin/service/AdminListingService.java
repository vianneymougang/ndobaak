package com.ndobaak.backend.admin.service;

import com.ndobaak.backend.admin.dto.RejectionMotifRequest;
import com.ndobaak.backend.common.enums.ListingStatus;
import com.ndobaak.backend.common.enums.NotificationType;
import com.ndobaak.backend.common.enums.RejectionMotif;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    @Transactional(readOnly = true)
    public Page<ListingResponse> listAllListings(String status, String search, Pageable pageable) {
        Specification<Listing> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            ListingStatus listingStatus = ListingStatus.valueOf(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), listingStatus));
        }

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.join("landlord").get("firstName")), pattern),
                    cb.like(cb.lower(root.join("landlord").get("lastName")), pattern)
            ));
        }

        return listingRepository.findAll(spec, pageable)
                .map(l -> listingMapper.toResponse(l, false));
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
            throw new BusinessException("Seules les annonces en attente de validation peuvent être approuvées.");
        }

        int photoCount = photoRepository.countByListingId(listingId);
        if (photoCount < 3 || photoCount > 10) {
            throw new BusinessException("L'annonce doit contenir entre 3 et 10 photos.");
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
    public ListingResponse rejectListing(Long listingId, RejectionMotifRequest request) {
        Listing listing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw new BusinessException("Seules les annonces en attente de validation peuvent être rejetées.");
        }

        String reason = buildRejectionMessage(request.getMotif(), request.getAdditionalReason());

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

    private String buildRejectionMessage(RejectionMotif motif, String additionalReason) {
        String base = switch (motif) {
            case INACCURATE_PHOTOS -> "Les photos ne correspondent pas au logement réel.";
            case MISLEADING_DESCRIPTION -> "La description est trompeuse ou inexacte.";
            case INVALID_PRICE -> "Le prix affiché est incorrect ou abusif.";
            case INCOMPLETE_INFORMATION -> "Les informations fournies sont incomplètes.";
            case DUPLICATE_LISTING -> "Cette annonce est un doublon d'une annonce existante.";
            case LOCATION_TOO_FAR -> "Le logement est situé en dehors de la zone autorisée (> 10 km du campus).";
            case UNVERIFIED_LANDLORD -> "Votre compte propriétaire n'est pas encore vérifié.";
            case OTHER -> additionalReason != null && !additionalReason.isBlank()
                    ? additionalReason
                    : "Motif non précisé.";
        };

        if (motif != RejectionMotif.OTHER && additionalReason != null && !additionalReason.isBlank()) {
            return base + " " + additionalReason;
        }

        return base;
    }
}
