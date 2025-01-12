package dev.robgro.timesheet.config;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FtpConfig {

    @Bean
    public FTPClient ftpClient() {
        return new FTPClient();
    }
}
