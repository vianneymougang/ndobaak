package com.ndobaak.backend.rentalcycle.service;

import com.ndobaak.backend.auth.entity.Landlord;
import com.ndobaak.backend.auth.entity.Student;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.booking.repository.BookingRepository;
import com.ndobaak.backend.common.enums.BookingStatus;
import com.ndobaak.backend.common.enums.LeaseStatus;
import com.ndobaak.backend.common.enums.UserRole;
import com.ndobaak.backend.common.exception.BusinessException;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.media.service.StorageService;
import com.ndobaak.backend.rentalcycle.dto.LeaseContractPdfData;
import com.ndobaak.backend.rentalcycle.dto.request.GenerateLeaseRequest;
import com.ndobaak.backend.rentalcycle.dto.response.LeaseContractResponse;
import com.ndobaak.backend.rentalcycle.entity.Guarantor;
import com.ndobaak.backend.rentalcycle.entity.LeaseContract;
import com.ndobaak.backend.rentalcycle.entity.TenantApplication;
import com.ndobaak.backend.rentalcycle.mapper.RentalCycleMapper;
import com.ndobaak.backend.rentalcycle.repository.LeaseContractRepository;
import com.ndobaak.backend.rentalcycle.repository.TenantApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaseContractService {

    private static final String AGENCY_NAME = "TANK'IMMO SAS";
    private static final String AGENCY_ADDRESS = "Eyang, Yaoundé — Cameroun";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LeaseContractRepository leaseContractRepository;
    private final TenantApplicationRepository applicationRepository;
    private final BookingRepository bookingRepository;
    private final StorageService storageService;
    private final ContractPdfGenerator pdfGenerator;
    private final RentalCycleMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public LeaseContractResponse generateContract(Long bookingId, GenerateLeaseRequest request, Long adminId) {
        Booking booking = findBooking(bookingId);

        if (booking.getStatus() != BookingStatus.APPLICATION_APPROVED) {
            throw new BusinessException(
                "Le contrat ne peut être généré que pour une réservation dont le dossier a été approuvé.");
        }

        if (leaseContractRepository.existsByBookingId(bookingId)) {
            throw new BusinessException("Un contrat de bail existe déjà pour cette réservation.");
        }

        TenantApplication application = applicationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier locataire introuvable pour cette réservation."));

        BigDecimal agencyFee = booking.getTotalAmount()
                .multiply(new BigDecimal("0.05"))
                .setScale(0, RoundingMode.CEILING);

        LeaseContractPdfData pdfData = buildPdfData(booking, application, request, agencyFee);
        byte[] pdfBytes = pdfGenerator.generate(pdfData);

        String pdfKey = storageService.uploadNamed(
                "lease-contracts",
                bookingId + ".pdf",
                new ByteArrayInputStream(pdfBytes),
                pdfBytes.length,
                "application/pdf"
        );

        BigDecimal monthlyRent = booking.getMonthlyRent() != null
                ? booking.getMonthlyRent() : booking.getTotalAmount();

        LeaseContract lease = new LeaseContract();
        lease.setBooking(booking);
        lease.setStartDate(request.startDate());
        lease.setEndDate(request.endDate());
        lease.setMonthlyRent(monthlyRent);
        lease.setSecurityDeposit(request.securityDeposit());
        lease.setAgencyFee(agencyFee);
        lease.setSpecialClauses(request.specialClauses());
        lease.setPdfUrl(pdfKey);
        lease.setStatus(LeaseStatus.AWAITING_LANDLORD_SIGNATURE);
        lease.setGeneratedById(adminId);

        leaseContractRepository.save(lease);

        booking.setStatus(BookingStatus.LEASE_GENERATED);
        bookingRepository.save(booking);

        return mapper.toResponse(lease);
    }

    public LeaseContractResponse getLease(Long bookingId, Long userId, UserRole role) {
        Booking booking = findBooking(bookingId);
        if (role != UserRole.ADMIN) {
            validateAccess(booking, userId);
        }

        LeaseContract lease = leaseContractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Contrat de bail introuvable pour cette réservation."));

        return mapper.toResponse(lease);
    }

    public byte[] getLeasePdf(Long bookingId, Long userId, UserRole role) {
        Booking booking = findBooking(bookingId);
        if (role != UserRole.ADMIN) {
            validateAccess(booking, userId);
        }

        LeaseContract lease = leaseContractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Contrat de bail introuvable pour cette réservation."));

        if (lease.getPdfUrl() == null) {
            throw new BusinessException("Le PDF de ce contrat n'est pas encore disponible.");
        }

        return storageService.download(lease.getPdfUrl());
    }

    @Transactional
    public LeaseContractResponse signContract(Long bookingId, Long userId, UserRole role) {
        Booking booking = findBooking(bookingId);

        LeaseContract lease = leaseContractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Contrat de bail introuvable pour cette réservation."));

        if (role == UserRole.LANDLORD) {
            Long landlordId = booking.getListing().getLandlord().getId();
            if (!landlordId.equals(userId)) {
                throw new AccessDeniedException("Vous n'êtes pas le propriétaire de ce logement.");
            }
            if (lease.getStatus() != LeaseStatus.AWAITING_LANDLORD_SIGNATURE) {
                throw new BusinessException("Ce contrat n'est pas en attente de votre signature.");
            }
            lease.setLandlordSignedAt(LocalDateTime.now());
            lease.setStatus(LeaseStatus.AWAITING_TENANT_SIGNATURE);

        } else if (role == UserRole.STUDENT) {
            Long studentId = booking.getStudent().getId();
            if (!studentId.equals(userId)) {
                throw new AccessDeniedException("Vous n'êtes pas le locataire de cette réservation.");
            }
            if (lease.getStatus() != LeaseStatus.AWAITING_TENANT_SIGNATURE) {
                throw new BusinessException("Le propriétaire doit signer le contrat en premier.");
            }
            lease.setTenantSignedAt(LocalDateTime.now());
            lease.setStatus(LeaseStatus.FULLY_SIGNED);
            booking.setStatus(BookingStatus.LEASE_SIGNED);
            bookingRepository.save(booking);

        } else {
            throw new AccessDeniedException("Seul le propriétaire ou le locataire peut signer ce contrat.");
        }

        leaseContractRepository.save(lease);
        return mapper.toResponse(lease);
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable."));
    }

    private void validateAccess(Booking booking, Long userId) {
        boolean isStudent = booking.getStudent().getId().equals(userId);
        boolean isLandlord = booking.getListing().getLandlord().getId().equals(userId);
        if (!isStudent && !isLandlord) {
            throw new AccessDeniedException("Accès non autorisé à ce contrat.");
        }
    }

    private LeaseContractPdfData buildPdfData(Booking booking, TenantApplication application,
                                               GenerateLeaseRequest request, BigDecimal agencyFee) {
        User student = booking.getStudent();
        Landlord landlord = (Landlord) booking.getListing().getLandlord();

        String studentMatricule = "";
        String studentFieldOfStudy = "";
        if (student instanceof Student s) {
            studentMatricule = s.getMatricule() != null ? s.getMatricule() : "";
            studentFieldOfStudy = s.getFieldOfStudy() != null ? s.getFieldOfStudy() : "";
        }

        List<Guarantor> guarantors = application.getGuarantors();
        String guarantorLastName = "";
        String guarantorFirstName = "";
        String guarantorIdCard = "";
        String guarantorPhone = "";
        String guarantorRelationship = "";

        if (!guarantors.isEmpty()) {
            Guarantor g = guarantors.get(0);
            guarantorLastName = g.getLastName();
            guarantorFirstName = g.getFirstName();
            guarantorIdCard = g.getIdCardNumber();
            guarantorPhone = g.getPhone();
            guarantorRelationship = g.getRelationshipToTenant();
        }

        String address = "";
        String city = "Yaoundé";
        if (booking.getListing().getLocation() != null) {
            var location = booking.getListing().getLocation();
            address = location.getAddress() != null ? location.getAddress() : "";
            city = location.getCity() != null ? location.getCity() : "Yaoundé";
        }

        String amenities = booking.getListing().getAmenities().stream()
                .map(a -> a.getName())
                .collect(Collectors.joining(", "));

        NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
        nf.setMaximumFractionDigits(0);

        BigDecimal monthlyRent = booking.getMonthlyRent() != null
                ? booking.getMonthlyRent() : booking.getTotalAmount();

        BigDecimal totalDue = monthlyRent.add(request.securityDeposit()).add(agencyFee);

        return new LeaseContractPdfData(
                landlord.getLastName(),
                landlord.getFirstName(),
                landlord.getIdCardNumber() != null ? landlord.getIdCardNumber() : "",
                landlord.getPhone() != null ? landlord.getPhone() : "",
                student.getLastName(),
                student.getFirstName(),
                studentMatricule,
                studentFieldOfStudy,
                student.getPhone() != null ? student.getPhone() : "",
                guarantorLastName,
                guarantorFirstName,
                guarantorIdCard,
                guarantorPhone,
                guarantorRelationship,
                address,
                city,
                booking.getListing().getDescription() != null ? booking.getListing().getDescription() : "",
                amenities,
                nf.format(monthlyRent),
                nf.format(request.securityDeposit()),
                nf.format(agencyFee),
                nf.format(totalDue),
                request.startDate().format(DATE_FORMATTER),
                request.endDate().format(DATE_FORMATTER),
                "NDK-" + booking.getId(),
                LocalDate.now().format(DATE_FORMATTER),
                AGENCY_NAME,
                AGENCY_ADDRESS
        );
    }
}
