package com.example.fintrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportTrendResponse(LocalDate period, BigDecimal income, BigDecimal expense) { }
