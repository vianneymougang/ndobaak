package com.ndobaak.backend.dashboard.dto;

public record BookingsByStatusDTO(long pending, long confirmed, long completed, long cancelled) {}
