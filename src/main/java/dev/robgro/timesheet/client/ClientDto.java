package dev.robgro.timesheet.client;

public record ClientDto(
        Long id,
        String clientName,
        double hourlyRate,
        long houseNo,
        String streetName,
        String city,
        String postCode,
        String email,
        boolean active
) {
}
