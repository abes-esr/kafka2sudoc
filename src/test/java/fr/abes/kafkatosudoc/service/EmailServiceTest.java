package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.kafka.WorkInProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    private static final String FILENAME =
            "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv";
    private static final String EMAIL_BODY =
            "1 erreur(s) lors du traitement sur le fichier " + FILENAME
                    + ". Rapport des erreurs de ce chargement en pièce jointe.";

    private CapturingEmailService emailService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        emailService = new CapturingEmailService(
                new ErrorWorkbookService(tempDir.toString()));
        setField("recipient", "recipient@abes.fr");
        setField("env", "test");
    }

    @Test
    void attachesInsertionWorkbookFor469Errors() throws IOException {
        WorkInProgress<LigneKbartConnect> work = connectWorkInProgress();
        work.addErrorMessages469(
                "262071878", work.getListeNotices().get(0), "",
                "Zone 469 invalide", ERROR_TYPE.ADD469);

        emailService.sendErrorsMessageCreateFromKafka(FILENAME, work);

        assertEquals("ErreursInsertion469.xlsx",
                emailService.attachment.getName());
        assertFalse(emailService.attachment.exists());
        assertEmail(
                "[KBART2SUDOC :  erreurs liens 469][TEST] " + FILENAME);
    }

    @Test
    void attachesCreationWorkbookForExNihiloErrors() throws IOException {
        WorkInProgress<LigneKbartConnect> work = connectWorkInProgress();
        work.addErrorMessageExNihilo("262071878", "Création impossible");

        emailService.sendErrorMessagesExNihilo(FILENAME, work);

        assertEquals("ErreursCreations.xlsx",
                emailService.attachment.getName());
        assertFalse(emailService.attachment.exists());
        assertEmail(
                "[KBART2SUDOC :  erreurs créations ex nihilo ][TEST] "
                        + FILENAME);
    }

    @Test
    void attachesCreationWorkbookForPrintedErrors() throws IOException {
        WorkInProgress<LigneKbartImprime> work = printedWorkInProgress();
        work.addErrorMessagesImprime("262071878", "Dérivation impossible");

        emailService.sendErrorMessagesImprime(FILENAME, work);

        assertEquals("ErreursCreations.xlsx",
                emailService.attachment.getName());
        assertFalse(emailService.attachment.exists());
        assertEmail(
                "[KBART2SUDOC :  erreurs créations par dérivations][TEST] "
                        + FILENAME);
    }

    @Test
    void doesNotSendEmailWhenNoRowIsAdded() throws IOException {
        emailService.sendErrorsMessageCreateFromKafka(
                FILENAME, connectWorkInProgress());

        assertNull(emailService.attachment);
        assertNull(emailService.requestJson);
        assertFalse(Files.exists(tempDir.resolve(
                "ErreursInsertion469.xlsx")));
    }

    private WorkInProgress<LigneKbartConnect> connectWorkInProgress() {
        LigneKbartConnect notice = new LigneKbartConnect();
        notice.setBESTPPN("262071878");
        notice.setPUBLICATIONTITLE("Titre électronique");
        notice.setPRINTIDENTIFIER("1234-5678");
        notice.setONLINEIDENTIFIER("8765-4321");
        WorkInProgress<LigneKbartConnect> work = new WorkInProgress<>(1);
        work.addNotice(notice);
        return work;
    }

    private WorkInProgress<LigneKbartImprime> printedWorkInProgress() {
        LigneKbartImprime notice = new LigneKbartImprime();
        notice.setPpn("262071878");
        notice.setPublicationTitle("Titre imprimé");
        notice.setPrintIdentifier("1234-5678");
        notice.setOnlineIdentifier("8765-4321");
        WorkInProgress<LigneKbartImprime> work = new WorkInProgress<>(1);
        work.addNotice(notice);
        return work;
    }

    private void assertEmail(String expectedSubject) throws IOException {
        JsonNode mail = new ObjectMapper().readTree(emailService.requestJson);
        assertEquals(expectedSubject, mail.get("subject").asText());
        assertEquals(EMAIL_BODY, mail.get("text").asText());
    }

    private void setField(String fieldName, String value)
            throws ReflectiveOperationException {
        Field field = EmailService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(emailService, value);
    }

    private static class CapturingEmailService extends EmailService {
        private File attachment;
        private String requestJson;

        CapturingEmailService(ErrorWorkbookService workbookService) {
            super(workbookService);
        }

        @Override
        protected void sendMailWithFile(String requestJson, File file) {
            this.requestJson = requestJson;
            this.attachment = file;
        }
    }
}
