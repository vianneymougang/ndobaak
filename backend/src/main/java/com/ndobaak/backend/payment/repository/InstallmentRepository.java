package com.ndobaak.backend.payment.repository;

import com.ndobaak.backend.common.enums.InstallmentStatus;
import com.ndobaak.backend.payment.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByPaymentScheduleId(Long paymentScheduleId);

    List<Installment> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDate date);
}
