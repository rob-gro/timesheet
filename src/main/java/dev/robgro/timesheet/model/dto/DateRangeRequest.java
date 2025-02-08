package dev.robgro.timesheet.model.dto;

public record DateRangeRequest(
        Integer fromYear,
        Integer fromMonth,
        Integer toYear,
        Integer toMonth
) {
}
