package com.ndobaak.backend.payment.service;

import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.auth.service.AuthService;
import com.ndobaak.backend.booking.entity.Booking;
import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.common.enums.PaymentStatus;
import com.ndobaak.backend.common.exception.ResourceNotFoundException;
import com.ndobaak.backend.payment.dto.PaymentScheduleResponse;
import com.ndobaak.backend.payment.entity.Installment;
import com.ndobaak.backend.payment.entity.Payment;
import com.ndobaak.backend.payment.entity.PaymentSchedule;
import com.ndobaak.backend.common.enums.ScheduleFrequency;
import com.ndobaak.backend.payment.mapper.PaymentMapper;
import com.ndobaak.backend.payment.repository.InstallmentRepository;
import com.ndobaak.backend.payment.repository.PaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentScheduleService {

    private static final Logger log = LoggerFactory.getLogger(PaymentScheduleService.class);

    private final PaymentScheduleRepository scheduleRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentQueryService queryService;
    private final AuthService authService;
    private final PaymentMapper paymentMapper;

    public PaymentScheduleService(PaymentScheduleRepository scheduleRepository,
                                  InstallmentRepository installmentRepository,
                                  PaymentQueryService queryService,
                                  AuthService authService,
                                  PaymentMapper paymentMapper) {
        this.scheduleRepository = scheduleRepository;
        this.installmentRepository = installmentRepository;
        this.queryService = queryService;
        this.authService = authService;
        this.paymentMapper = paymentMapper;
    }

    @Transactional(readOnly = true)
    public PaymentScheduleResponse getPaymentSchedule(Long bookingId) {
        User currentUser = authService.getAuthenticatedUser();
        Booking booking = queryService.findBookingById(bookingId);
        queryService.validateBookingAccess(booking, currentUser);

        PaymentSchedule schedule = scheduleRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun échéancier trouvé pour cette réservation."));

        return paymentMapper.toScheduleResponse(schedule);
    }

    void createBlockSchedule(Booking booking, BigDecimal totalAmount, BigDecimal blockSize, Payment firstPayment) {
        int blockCount = totalAmount.divide(blockSize, 0, RoundingMode.CEILING).intValue();

        PaymentSchedule schedule = PaymentSchedule.builder()
                .booking(booking)
                .totalAmount(totalAmount)
                .frequency(ScheduleFrequency.BLOCK)
                .numberOfInstallments(blockCount)
                .build();

        schedule = scheduleRepository.save(schedule);

        List<Installment> blocks = new ArrayList<>();
        BigDecimal remaining = totalAmount;

        for (int i = 1; i <= blockCount; i++) {
            BigDecimal blockAmount = remaining.min(blockSize);
            remaining = remaining.subtract(blockAmount);

            Installment block = Installment.builder()
                    .paymentSchedule(schedule)
                    .installmentNumber(i)
                    .amount(blockAmount)
                    .status(InstallmentStatus.PENDING)
                    .build();

            if (i == 1) {
                block.setPayment(firstPayment);
            }

            blocks.add(installmentRepository.save(block));
        }

        schedule.setInstallments(blocks);
        scheduleRepository.save(schedule);
    }

    void cleanupFailedScheduleIfPresent(Booking booking) {
        scheduleRepository.findByBookingId(booking.getId()).ifPresent(schedule -> {
            boolean allFailed = schedule.getInstallments().stream()
                    .allMatch(i -> i.getPayment() == null
                            || i.getPayment().getStatus() == PaymentStatus.FAILED);
            if (allFailed) {
                installmentRepository.deleteAll(schedule.getInstallments());
                scheduleRepository.delete(schedule);
                log.info("Cleaned up fully-failed block schedule for booking={}", booking.getId());
            }
        });
    }
}
