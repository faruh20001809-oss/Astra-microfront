package ru.astrakhan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatsDto {
    private long publishedPois;
    private long publishedRoutes;
    private long products;
    private long orders;
    private long feedbackCount;
    private long poiSuggestionsCount;
}
