package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.FtpException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
@Getter
public class FtpService {

    private final Environment environment;

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

    @Value("${ftp.connection.timeout:10000}")
    private int connectionTimeout;

    @Value("${ftp.data.timeout:60000}")
    private int dataTimeout;

    @Value("${ftp.security.accept-all-certificates:false}")
    private boolean acceptAllCertificates;

    public FtpService(Environment environment) {
        this.environment = environment;
    }

    public void uploadPdfInvoice(String fileName, byte[] content) {
        log.info("Starting upload of invoice PDF: {}", fileName);
        int maxRetries = 3;
        int retryCount = 0;
        int backoffMs = 1000;

        while (retryCount < maxRetries) {
            try {
                doUploadPdfInvoice(fileName, content);
                return;
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("Failed to upload PDF after {} attempts", maxRetries, e);
                    throw new FtpException("Failed to upload invoice PDF to FTP after " + maxRetries + " attempts", e);
                }

                log.warn("Upload attempt {} failed, retrying in {} ms", retryCount, backoffMs, e);
                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new FtpException("Interrupted during retry", ie);
                }
            }
        }
    }

    private void doUploadPdfInvoice(String fileName, byte[] content) throws IOException {
        FTPSClient ftpsClient = null;

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            ftpsClient = createFtpsClient();
            connectToFtp(ftpsClient);

            // Passive mode ON
            ftpsClient.enterLocalPassiveMode();
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);

            boolean success = ftpsClient.storeFile(invoicesDirectory + "/" + fileName, inputStream);
            logFtpResponse(ftpsClient, "Upload file");

            if (!success) {
                throw new IOException("Failed to upload invoice PDF: " + ftpsClient.getReplyString());
            }

            log.info("Successfully uploaded invoice PDF: {}", fileName);
        } finally {
            disconnect(ftpsClient);
        }
    }

    public byte[] downloadPdfInvoice(String fileName) {
        log.info("Starting download of invoice PDF: {}", fileName);
        int maxRetries = 3;
        int retryCount = 0;
        int backoffMs = 1000;

        while (retryCount < maxRetries) {
            try {
                return doDownloadPdfInvoice(fileName);
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("Failed to download PDF after {} attempts", maxRetries, e);
                    throw new FtpException("Failed to download invoice PDF from FTP after " + maxRetries + " attempts", e);
                }

                log.warn("Download attempt {} failed, retrying in {} ms", retryCount, backoffMs, e);
                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new FtpException("Interrupted during retry", ie);
                }
            }
        }
        throw new FtpException("Unexpected code path in downloadPdfInvoice");
    }

    private byte[] doDownloadPdfInvoice(String fileName) throws IOException {
        FTPSClient ftpsClient = null;

        try {
            ftpsClient = createFtpsClient();
            connectToFtp(ftpsClient);

            ftpsClient.enterLocalPassiveMode();
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpsClient.retrieveFile(
                    invoicesDirectory + "/" + fileName,
                    outputStream
            );

            logFtpResponse(ftpsClient, "Download file");

            if (!success) {
                throw new FtpException("Failed to download invoice PDF: " + ftpsClient.getReplyString());
            }

            log.info("Successfully downloaded invoice PDF: {}", fileName);
            return outputStream.toByteArray();
        } finally {
            disconnect(ftpsClient);
        }
    }

    private FTPSClient createFtpsClient() {
        FTPSClient ftpsClient = new FTPSClient(false); // Explicit mode

        ftpsClient.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.3" });

        ftpsClient.setConnectTimeout(connectionTimeout);
        ftpsClient.setDefaultTimeout(connectionTimeout);
        ftpsClient.setDataTimeout(Duration.ofMillis(dataTimeout));

        if (isAcceptAllCertificates()) {
            ftpsClient.setTrustManager(createAllTrustingManager());
            ftpsClient.setHostnameVerifier((hostname, session) -> true);
            log.warn("Using insecure certificate validation - NOT FOR PRODUCTION USE");
        }

        return ftpsClient;
    }

    private void connectToFtp(FTPSClient ftpsClient) throws IOException {
        log.debug("Connecting to FTP server: {}:{}", server, port);

        try {
            ftpsClient.connect(server, port);
            logFtpResponse(ftpsClient, "Connect");

            int reply = ftpsClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("Failed to connect to FTP server. Reply code: " + reply);
            }

            if (!ftpsClient.login(username, password)) {
                logFtpResponse(ftpsClient, "Login");
                throw new IOException("Failed to login to FTP server. Invalid credentials.");
            }

            logFtpResponse(ftpsClient, "Login");

            // Protection Buffer Size
            ftpsClient.execPBSZ(0);
            logFtpResponse(ftpsClient, "PBSZ");

            //  Data Channel Protection Level (private)
            ftpsClient.execPROT("P");
            logFtpResponse(ftpsClient, "PROT");

            log.debug("Successfully connected to FTP server");
        } catch (IOException e) {
            log.error("Failed to connect to FTP server: {}:{}", server, port, e);
            throw new FtpException("Connection to FTP server failed: " + e.getMessage(), e);
        }
    }

    private void disconnect(FTPSClient ftpsClient) {
        if (ftpsClient == null) {
            return;
        }

        try {
            if (ftpsClient.isConnected()) {
                ftpsClient.logout();
                logFtpResponse(ftpsClient, "Logout");
                ftpsClient.disconnect();
                log.debug("Disconnected from FTP server");
            }
        } catch (IOException e) {
            log.warn("Error disconnecting from FTP server", e);
        }
    }

    private boolean isAcceptAllCertificates() {
        return acceptAllCertificates && !isProductionEnvironment();
    }

    private boolean isProductionEnvironment() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private boolean isDevelopmentEnvironment() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                Arrays.asList(environment.getActiveProfiles()).contains("local") ||
                Arrays.asList(environment.getDefaultProfiles()).contains("default");
    }

    private TrustManager createAllTrustingManager() {
        return new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        };
    }

    private void logFtpResponse(FTPSClient ftpsClient, String operation) {
        log.debug("{} result: {} - {}",
                operation,
                ftpsClient.getReplyCode(),
                ftpsClient.getReplyString().trim());
    }
}
