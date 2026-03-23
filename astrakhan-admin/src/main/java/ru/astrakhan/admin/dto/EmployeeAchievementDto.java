package ru.astrakhan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAchievementDto {
    private String id;
    private String title;
    private String description;
    /** LOCKED | IN_PROGRESS | UNLOCKED */
    private String status;
    /** 0–100 */
    private int progress;
}
