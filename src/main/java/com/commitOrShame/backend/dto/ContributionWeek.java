package com.commitOrShame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContributionWeek {
    private List<ContributionDay> contributionDays;
}