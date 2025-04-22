package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.FtpException;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FtpServiceTest {

    private static final String SERVER = "ftp.example.com";
    private static final int PORT = 21;
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String INVOICES_DIR = "/invoices";

    @Test
    void shouldUploadAndDownloadPdfInvoice() {
        // given
        InMemoryFtpService ftpService = createInMemoryFtpService();
        String fileName = "test-invoice.pdf";
        byte[] content = "PDF content for testing".getBytes(StandardCharsets.UTF_8);

        // when
        ftpService.uploadPdfInvoice(fileName, content);
        byte[] downloadedContent = ftpService.downloadPdfInvoice(fileName);

        // then
        assertThat(downloadedContent).isEqualTo(content);
    }

    @Test
    void shouldHandleFileNotFound() {
        // given
        InMemoryFtpService ftpService = createInMemoryFtpService();
        String nonExistentFile = "non-existent-file.pdf";

        // when/then
        assertThatThrownBy(() -> ftpService.downloadPdfInvoice(nonExistentFile))
                .isInstanceOf(FtpException.class)
                .hasMessageContaining("Failed to download invoice PDF");
    }

    @Test
    void shouldHandleMultipleUploads() {
        // given
        InMemoryFtpService ftpService = createInMemoryFtpService();
        String fileName1 = "invoice1.pdf";
        String fileName2 = "invoice2.pdf";
        byte[] content1 = "PDF content 1".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "PDF content 2".getBytes(StandardCharsets.UTF_8);

        // when
        ftpService.uploadPdfInvoice(fileName1, content1);
        ftpService.uploadPdfInvoice(fileName2, content2);

        // then
        byte[] downloadedContent1 = ftpService.downloadPdfInvoice(fileName1);
        byte[] downloadedContent2 = ftpService.downloadPdfInvoice(fileName2);

        assertThat(downloadedContent1).isEqualTo(content1);
        assertThat(downloadedContent2).isEqualTo(content2);
    }

    @Test
    void shouldHandleRetry() {
        // given
        // Tworzymy instancję podklasy FtpService, która nadpisuje metodę doDownloadPdfInvoice
        FtpService ftpService = new FtpService(mock(Environment.class)) {
            private int attemptCount = 0;

            @Override
            protected byte[] doDownloadPdfInvoice(String fileName) throws IOException {
                attemptCount++;
                if (attemptCount <= 2) {
                    // Rzucamy IOException, który zostanie złapany przez mechanizm retry
                    // i opakowany w FtpException
                    throw new IOException("Simulated download failure");
                }
                return "Success after retry".getBytes();
            }
        };

        // Konfiguracja pól
        ReflectionTestUtils.setField(ftpService, "server", "ftp.example.com");
        ReflectionTestUtils.setField(ftpService, "port", 21);
        ReflectionTestUtils.setField(ftpService, "username", "testuser");
        ReflectionTestUtils.setField(ftpService, "password", "password123");
        ReflectionTestUtils.setField(ftpService, "invoicesDirectory", "/invoices");

        // Mockujemy Environment
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getActiveProfiles()).thenReturn(new String[]{"test"});
        ReflectionTestUtils.setField(ftpService, "environment", mockEnv);

        // when
        byte[] result = ftpService.downloadPdfInvoice("test.pdf");

        // then
        assertThat(result).isEqualTo("Success after retry".getBytes());
    }

    @Test
    void shouldHandleMaxRetriesExceeded() {
        // given
        // Tworzymy instancję podklasy FtpService, która zawsze rzuca wyjątek
        FtpService ftpService = new FtpService(mock(Environment.class)) {
            @Override
            protected byte[] doDownloadPdfInvoice(String fileName) throws IOException {
                // Zawsze rzucamy wyjątek, co spowoduje wyczerpanie limitu prób
                throw new IOException("Persistent failure");
            }
        };

        // Konfiguracja pól
        ReflectionTestUtils.setField(ftpService, "server", "ftp.example.com");
        ReflectionTestUtils.setField(ftpService, "port", 21);
        ReflectionTestUtils.setField(ftpService, "username", "testuser");
        ReflectionTestUtils.setField(ftpService, "password", "password123");
        ReflectionTestUtils.setField(ftpService, "invoicesDirectory", "/invoices");

        // Mockujemy Environment
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getActiveProfiles()).thenReturn(new String[]{"test"});
        ReflectionTestUtils.setField(ftpService, "environment", mockEnv);

        // when/then
        // Spodziewamy się własnego wyjątku FtpException, a nie standardowego IOException
        assertThatThrownBy(() -> ftpService.downloadPdfInvoice("test.pdf"))
                .isInstanceOf(dev.robgro.timesheet.exception.FtpException.class)
                .hasMessageContaining("Failed to download invoice PDF");
    }

//    @Test
//    void shouldHandleMaxRetriesExceeded() {
//        // given
//        FailingFtpService ftpService = new FailingFtpService();
//        configureService(ftpService);
//
//        // when/then
//        assertThatThrownBy(() -> ftpService.downloadPdfInvoice("failing-test.pdf"))
//                .isInstanceOf(FtpException.class)
//                .hasMessageContaining("Simulated persistent failure");
//    }

    // Helpers

    private InMemoryFtpService createInMemoryFtpService() {
        InMemoryFtpService service = new InMemoryFtpService();
        configureService(service);
        return service;
    }

    private void configureService(FtpService service) {
        // Konfiguracja pól przez refleksję
        ReflectionTestUtils.setField(service, "server", SERVER);
        ReflectionTestUtils.setField(service, "port", PORT);
        ReflectionTestUtils.setField(service, "username", USERNAME);
        ReflectionTestUtils.setField(service, "password", PASSWORD);
        ReflectionTestUtils.setField(service, "invoicesDirectory", INVOICES_DIR);
        ReflectionTestUtils.setField(service, "connectionTimeout", 5000);
        ReflectionTestUtils.setField(service, "dataTimeout", 10000);
        ReflectionTestUtils.setField(service, "acceptAllCertificates", false);

        // Ustawienie środowiska
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getActiveProfiles()).thenReturn(new String[]{"test"});
        ReflectionTestUtils.setField(service, "environment", mockEnv);
    }

    // Test implementations

    private static class InMemoryFtpService extends FtpService {
        private final Map<String, byte[]> fileStorage = new HashMap<>();

        public InMemoryFtpService() {
            // Pusty konstruktor, przekażemy fałszywe środowisko przez refleksję
            super(null);
        }

        @Override
        public void uploadPdfInvoice(String fileName, byte[] content) {
            String fullPath = getInvoicesDirectory() + "/" + fileName;
            fileStorage.put(fullPath, content);
        }

        @Override
        public byte[] downloadPdfInvoice(String fileName) {
            String fullPath = getInvoicesDirectory() + "/" + fileName;
            if (!fileStorage.containsKey(fullPath)) {
                throw new FtpException("Failed to download invoice PDF: File not found");
            }
            return fileStorage.get(fullPath);
        }
    }

    private static class RetrySimulatingFtpService extends FtpService {
        private int downloadAttempts = 0;
        private final int failuresBeforeSuccess;

        public RetrySimulatingFtpService(int failuresBeforeSuccess) {
            super(null); // Przekażemy fałszywe środowisko przez refleksję
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        public byte[] downloadPdfInvoice(String fileName) {
            downloadAttempts++;
            if (downloadAttempts <= failuresBeforeSuccess) {
                throw new FtpException("Simulated download failure");
            }
            return "Success after retry".getBytes(StandardCharsets.UTF_8);
        }

        public int getDownloadAttempts() {
            return downloadAttempts;
        }
    }

    private static class FailingFtpService extends FtpService {
        public FailingFtpService() {
            super(null); // Przekażemy fałszywe środowisko przez refleksję
        }

        @Override
        public byte[] downloadPdfInvoice(String fileName) {
            throw new FtpException("Simulated persistent failure");
        }
    }
}
