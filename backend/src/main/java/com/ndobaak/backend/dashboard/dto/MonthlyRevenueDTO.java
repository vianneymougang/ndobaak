package com.ndobaak.backend.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyRevenueDTO(String month, BigDecimal revenue, int bookingCount) {}
