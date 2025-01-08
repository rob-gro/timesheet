package dev.robgro.timesheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimesheetApplication {

    public static void main(String[] args) {
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080";
        }
        System.setProperty("server.port", port);
        SpringApplication.run(TimesheetApplication.class, args);
    }

}
