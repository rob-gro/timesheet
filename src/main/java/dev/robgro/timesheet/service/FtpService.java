package dev.robgro.timesheet.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class FtpService {

    private final FTPClient ftpClient;

    @Value("${ftp.server}")
    private String server;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;

    @Value("${ftp.invoices.directory}")
    private String invoicesDirectory;

    public FtpService() {
        this.ftpClient = new FTPClient();
    }

    public void uploadPdfInvoice(String fileName, byte[] content) {
        log.info("Starting upload of invoice PDF: {}", fileName);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            connectToFtp();
            boolean success = ftpClient.storeFile(invoicesDirectory + "/" + fileName, inputStream);
            if (!success) {
                log.error("Failed to upload invoice PDF: {}", fileName);
                throw new RuntimeException("Failed to upload invoice PDF to FTP");
            }
            log.info("Successfully uploaded invoice PDF: {}", fileName);
        } catch (IOException e) {
            log.error("Error uploading invoice PDF: {}", fileName, e);
            throw new RuntimeException("Failed to upload invoice PDF to FTP", e);
        } finally {
            disconnect();
        }
    }

    public byte[] downloadPdfInvoice(String fileName) {
        log.info("Starting download of invoice PDF: {}", fileName);
        try {
            connectToFtp();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(
                    invoicesDirectory + "/" + fileName,
                    outputStream
            );

            if (!success) {
                log.error("Failed to download invoice PDF: {}", fileName);
                throw new RuntimeException("Failed to download invoice PDF from FTP");
            }

            log.info("Successfully downloaded invoice PDF: {}", fileName);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Error downloading invoice PDF: {}", fileName, e);
            throw new RuntimeException("Failed to download invoice PDF from FTP", e);
        } finally {
            disconnect();
        }
    }

    private void connectToFtp() throws IOException {
        log.debug("Connecting to FTP server: {}", server);
        ftpClient.connect(server, port);
        ftpClient.login(username, password);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        log.debug("Successfully connected to FTP server");
    }

    private void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
                log.debug("Disconnected from FTP server");
            }
        } catch (IOException e) {
            log.warn("Error disconnecting from FTP server", e);
        }
    }
}
