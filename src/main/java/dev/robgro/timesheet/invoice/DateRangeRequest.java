package dev.robgro.timesheet.invoice;

public record DateRangeRequest(
        Integer fromYear,
        Integer fromMonth,
        Integer toYear,
        Integer toMonth
) {
}
