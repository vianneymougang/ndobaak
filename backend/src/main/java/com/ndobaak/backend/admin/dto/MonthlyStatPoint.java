package com.ndobaak.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyStatPoint {
    private String month;
    private Number value;
}
