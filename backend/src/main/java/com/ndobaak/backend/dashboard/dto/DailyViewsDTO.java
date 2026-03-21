package com.ndobaak.backend.dashboard.dto;

import java.time.LocalDate;

public record DailyViewsDTO(LocalDate date, long viewCount) {}
