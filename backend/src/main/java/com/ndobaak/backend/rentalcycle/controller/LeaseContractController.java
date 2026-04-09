package com.ndobaak.backend.rentalcycle.controller;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.rentalcycle.dto.response.LeaseContractResponse;
import com.ndobaak.backend.rentalcycle.service.LeaseContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class LeaseContractController {

    private final LeaseContractService leaseContractService;
    private final AuthService authService;

    @GetMapping("/{id}/lease")
    @PreAuthorize("hasAnyRole('STUDENT', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<LeaseContractResponse> getLease(@PathVariable Long id) {
        User user = authService.getAuthenticatedUser();
        return ResponseEntity.ok(leaseContractService.getLease(id, user.getId()));
    }

    @GetMapping("/{id}/lease/pdf")
    @PreAuthorize("hasAnyRole('STUDENT', 'LANDLORD', 'ADMIN')")
    public ResponseEntity<byte[]> getLeasePdf(@PathVariable Long id) {
        User user = authService.getAuthenticatedUser();
        byte[] pdfBytes = leaseContractService.getLeasePdf(id, user.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"contrat-bail-" + id + ".pdf\"")
                .body(pdfBytes);
    }

    @PostMapping("/{id}/lease/sign")
    @PreAuthorize("hasAnyRole('LANDLORD', 'STUDENT')")
    public ResponseEntity<LeaseContractResponse> signLease(@PathVariable Long id) {
        User user = authService.getAuthenticatedUser();
        return ResponseEntity.ok(leaseContractService.signContract(id, user.getId(), user.getRole()));
    }
}
