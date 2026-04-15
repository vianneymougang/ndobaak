package com.ndobaak.backend.rentalcycle.service;

import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.InspectionStatus;
import com.ndobaak.backend.common.enums.InspectionType;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.common.util.FileValidator;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.rentalcycle.dto.request.InspectionItemRequest;
import com.ndobaak.backend.rentalcycle.dto.request.InspectionRequest;
import com.ndobaak.backend.rentalcycle.dto.response.InspectionItemResponse;
import com.ndobaak.backend.rentalcycle.dto.response.InspectionPhotoResponse;
import com.ndobaak.backend.rentalcycle.dto.response.PropertyInspectionResponse;
import com.ndobaak.backend.rentalcycle.entity.InspectionItem;
import com.ndobaak.backend.rentalcycle.entity.InspectionPhoto;
import com.ndobaak.backend.rentalcycle.entity.PropertyInspection;
import com.ndobaak.backend.rentalcycle.mapper.RentalCycleMapper;
import com.ndobaak.backend.rentalcycle.repository.InspectionItemRepository;
import com.ndobaak.backend.rentalcycle.repository.InspectionPhotoRepository;
import com.ndobaak.backend.rentalcycle.repository.PropertyInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyInspectionService {

    private final PropertyInspectionRepository inspectionRepository;
    private final InspectionItemRepository itemRepository;
    private final InspectionPhotoRepository photoRepository;
    private final BookingRepository bookingRepository;
    private final StorageService storageService;
    private final RentalCycleMapper mapper;

    @Transactional
    public PropertyInspectionResponse createInspection(Long bookingId, InspectionRequest request, Long adminId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable."));

        if (booking.getStatus() != BookingStatus.PAYMENT_HELD) {
            throw new BusinessException(
                    "L'état des lieux ne peut être créé que pour une réservation avec paiement bloqué (statut PAYMENT_HELD).");
        }

        if (inspectionRepository.existsByBookingIdAndType(bookingId, request.type())) {
            throw new BusinessException("Un état des lieux de type " + request.type() + " existe déjà pour cette réservation.");
        }

        PropertyInspection inspection = new PropertyInspection();
        inspection.setBooking(booking);
        inspection.setType(request.type());
        inspection.setConductedAt(request.conductedAt());
        inspection.setAgentId(adminId);
        inspection.setStatus(InspectionStatus.IN_PROGRESS);
        inspection.setLandlordSigned(false);
        inspection.setTenantSigned(false);
        inspection.setObservations(request.observations());

        return mapper.toResponse(inspectionRepository.save(inspection));
    }

    @Transactional
    public InspectionItemResponse addItem(Long bookingId, Long inspectionId, InspectionItemRequest request) {
        PropertyInspection inspection = findInspectionForBooking(bookingId, inspectionId);

        if (inspection.getStatus() == InspectionStatus.FULLY_SIGNED) {
            throw new BusinessException("Impossible d'ajouter des éléments à un état des lieux déjà signé.");
        }

        InspectionItem item = new InspectionItem();
        item.setInspection(inspection);
        item.setLocation(request.location());
        item.setElement(request.element());
        item.setCondition(request.condition());
        item.setObservations(request.observations());

        return mapper.toResponse(itemRepository.save(item));
    }

    @Transactional
    public InspectionPhotoResponse uploadPhoto(Long bookingId, Long inspectionId,
                                               MultipartFile file, Long itemId) {
        PropertyInspection inspection = findInspectionForBooking(bookingId, inspectionId);

        InspectionItem item = null;
        if (itemId != null) {
            item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Élément d'état des lieux introuvable."));
            if (!item.getInspection().getId().equals(inspectionId)) {
                throw new BusinessException("Cet élément n'appartient pas à cet état des lieux.");
            }
        }

        String storageUrl;
        try {
            FileValidator.validateImageFile(file);
            storageUrl = storageService.upload(
                    "inspection-photos",
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new BusinessException("Erreur lors de l'upload de la photo.");
        }

        InspectionPhoto photo = new InspectionPhoto();
        photo.setInspection(inspection);
        photo.setItem(item);
        photo.setStorageUrl(storageUrl);

        return mapper.toResponse(photoRepository.save(photo));
    }

    @Transactional
    public PropertyInspectionResponse signInspection(Long bookingId, Long inspectionId,
                                                      Long userId, UserRole role) {
        PropertyInspection inspection = findInspectionForBooking(bookingId, inspectionId);

        if (inspection.getStatus() == InspectionStatus.FULLY_SIGNED) {
            throw new BusinessException("Cet état des lieux a déjà été signé par les deux parties.");
        }

        Booking booking = inspection.getBooking();

        if (role == UserRole.LANDLORD) {
            if (!booking.getListing().getLandlord().getId().equals(userId)) {
                throw new AccessDeniedException("Vous ne pouvez signer que les états des lieux de vos logements.");
            }
            if (inspection.isLandlordSigned()) {
                throw new BusinessException("Vous avez déjà signé cet état des lieux.");
            }
            inspection.setLandlordSigned(true);
            inspection.setLandlordSignedAt(LocalDateTime.now());
            inspection.setStatus(InspectionStatus.LANDLORD_SIGNED);

        } else if (role == UserRole.STUDENT) {
            if (!booking.getStudent().getId().equals(userId)) {
                throw new AccessDeniedException("Vous ne pouvez signer que vos propres états des lieux.");
            }
            if (!inspection.isLandlordSigned()) {
                throw new BusinessException(
                        "Le propriétaire doit signer l'état des lieux en premier.");
            }
            if (inspection.isTenantSigned()) {
                throw new BusinessException("Vous avez déjà signé cet état des lieux.");
            }
            inspection.setTenantSigned(true);
            inspection.setTenantSignedAt(LocalDateTime.now());
            inspection.setStatus(InspectionStatus.FULLY_SIGNED);

        } else {
            throw new AccessDeniedException("Seuls le propriétaire et le locataire peuvent signer l'état des lieux.");
        }

        return mapper.toResponse(inspectionRepository.save(inspection));
    }

    @Transactional(readOnly = true)
    public PropertyInspectionResponse getInspection(Long bookingId, Long inspectionId) {
        return mapper.toResponse(findInspectionForBooking(bookingId, inspectionId));
    }

    @Transactional(readOnly = true)
    public List<PropertyInspectionResponse> getInspections(Long bookingId) {
        return inspectionRepository.findByBookingId(bookingId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private PropertyInspection findInspectionForBooking(Long bookingId, Long inspectionId) {
        PropertyInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("État des lieux introuvable."));
        if (!inspection.getBooking().getId().equals(bookingId)) {
            throw new BusinessException("Cet état des lieux n'appartient pas à cette réservation.");
        }
        return inspection;
    }
}
