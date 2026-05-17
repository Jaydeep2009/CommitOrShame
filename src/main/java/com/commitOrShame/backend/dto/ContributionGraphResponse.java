package com.commitOrShame.backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContributionGraphResponse {
    private String username;
    private int totalContributions;
    private List<ContributionWeek> weeks;
}