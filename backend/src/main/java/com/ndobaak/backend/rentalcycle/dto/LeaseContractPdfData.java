package com.ndobaak.backend.rentalcycle.dto;

public record LeaseContractPdfData(
        String landlordLastName,
        String landlordFirstName,
        String landlordIdCardNumber,
        String tenantLastName,
        String tenantFirstName,
        String tenantMatricule,
        String tenantFieldOfStudy,
        String guarantorLastName,
        String guarantorFirstName,
        String guarantorIdCard,
        String guarantorPhone,
        String guarantorRelationship,
        String propertyAddress,
        String propertyCity,
        String propertyDescription,
        String amenities,
        String formattedMonthlyRent,
        String formattedSecurityDeposit,
        String formattedAgencyFee,
        String startDate,
        String endDate,
        String bookingReference,
        String generationDate,
        String agencyName,
        String agencyAddress
) {
}
