package ads.uninassau.brjobs.dto;

import lombok.Data;

@Data
public class HighlightPlanDTO {
    private Long id;
    private String name;
    private Double price;
    private Integer durationDays;
    private Integer priority;
}
