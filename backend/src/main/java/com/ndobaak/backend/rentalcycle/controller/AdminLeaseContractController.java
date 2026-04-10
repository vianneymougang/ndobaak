package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.request.GenerateLeaseRequest;
import com.ndobaak.backend.rentalcycle.dto.response.LeaseContractResponse;
import com.ndobaak.backend.rentalcycle.service.LeaseContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLeaseContractController {

    private final LeaseContractService leaseContractService;
    private final AuthService authService;

    @PostMapping("/{id}/lease")
    public ResponseEntity<LeaseContractResponse> generateLease(
            @PathVariable Long id,
            @Valid @RequestBody GenerateLeaseRequest request) {
        Long adminId = authService.getAuthenticatedUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaseContractService.generateContract(id, request, adminId));
    }
}
