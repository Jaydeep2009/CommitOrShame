package com.commitOrShame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContributionDay {
    private String date;        // "2024-05-17"
    private int contributionCount;
    private String color;       // GitHub's hex color e.g. "#216e39"
}