package dev.robgro.timesheet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "invoice.seller")
@Getter
@Setter
public class InvoiceSeller {
    private String name = "Agnieszka Markiewicz";
    private String street = "29 Forth Crescent";
    private String postcode = "DD2 4JB";
//    private String street = "28 Ballater Place";
//    private String postcode = "DD4 8SF";
    private String city = "Dundee";
}
