package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.abes.LigneKbartConnect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class WorkInProgressTest {

    @Test
    void getAllErrorMessages() throws IOException {
        String messageReference = "[\"Provider : FICHIER\",\"Package : TEST_TEST-SANS-OBJET\",\"Date : 2024-01-01\",\"2 erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets) : [connectionCBS_1, connectionCBS_2]\\r\\n\",\"2 erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets) : [Date_1, Date_2]\\r\\n\",\"2 erreur(s) d'ajout de 469 : [[PPN : ppn, Ligne Kbart : {\\\"PUBLICATION_TITLE\\\": null, \\\"PRINT_IDENTIFIER\\\": null, \\\"ONLINE_IDENTIFIER\\\": null, \\\"DATE_FIRST_ISSUE_ONLINE\\\": null, \\\"NUM_FIRST_VOL_ONLINE\\\": null, \\\"NUM_FIRST_ISSUE_ONLINE\\\": null, \\\"DATE_LAST_ISSUE_ONLINE\\\": null, \\\"NUM_LAST_VOL_ONLINE\\\": null, \\\"NUM_LAST_ISSUE_ONLINE\\\": null, \\\"TITLE_URL\\\": null, \\\"FIRST_AUTHOR\\\": null, \\\"TITLE_ID\\\": null, \\\"EMBARGO_INFO\\\": null, \\\"COVERAGE_DEPTH\\\": null, \\\"NOTES\\\": null, \\\"PUBLISHER_NAME\\\": null, \\\"PUBLICATION_TYPE\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_PRINT\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_ONLIN\\\": null, \\\"MONOGRAPH_VOLUME\\\": null, \\\"MONOGRAPH_EDITION\\\": null, \\\"FIRST_EDITOR\\\": null, \\\"PARENT_PUBLICATION_TITLE_ID\\\": null, \\\"PRECEDING_PUBLICATION_TITLE_ID\\\": null, \\\"ACCESS_TYPE\\\": null, \\\"PROVIDER_PACKAGE_PACKAGE\\\": null, \\\"PROVIDER_PACKAGE_DATE_P\\\": null, \\\"PROVIDER_PACKAGE_IDT_PROVIDER\\\": 0, \\\"ID_PROVIDER_PACKAGE\\\": 0, \\\"BEST_PPN\\\": null}, Erreur : erreur], [PPN : ppn, Ligne Kbart : {\\\"PUBLICATION_TITLE\\\": null, \\\"PRINT_IDENTIFIER\\\": null, \\\"ONLINE_IDENTIFIER\\\": null, \\\"DATE_FIRST_ISSUE_ONLINE\\\": null, \\\"NUM_FIRST_VOL_ONLINE\\\": null, \\\"NUM_FIRST_ISSUE_ONLINE\\\": null, \\\"DATE_LAST_ISSUE_ONLINE\\\": null, \\\"NUM_LAST_VOL_ONLINE\\\": null, \\\"NUM_LAST_ISSUE_ONLINE\\\": null, \\\"TITLE_URL\\\": null, \\\"FIRST_AUTHOR\\\": null, \\\"TITLE_ID\\\": null, \\\"EMBARGO_INFO\\\": null, \\\"COVERAGE_DEPTH\\\": null, \\\"NOTES\\\": null, \\\"PUBLISHER_NAME\\\": null, \\\"PUBLICATION_TYPE\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_PRINT\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_ONLIN\\\": null, \\\"MONOGRAPH_VOLUME\\\": null, \\\"MONOGRAPH_EDITION\\\": null, \\\"FIRST_EDITOR\\\": null, \\\"PARENT_PUBLICATION_TITLE_ID\\\": null, \\\"PRECEDING_PUBLICATION_TITLE_ID\\\": null, \\\"ACCESS_TYPE\\\": null, \\\"PROVIDER_PACKAGE_PACKAGE\\\": null, \\\"PROVIDER_PACKAGE_DATE_P\\\": null, \\\"PROVIDER_PACKAGE_IDT_PROVIDER\\\": 0, \\\"ID_PROVIDER_PACKAGE\\\": 0, \\\"BEST_PPN\\\": null}, Erreur : erreur]]\",\"2 erreur(s) de suppression de 469 : [[PPN : ppn, Ligne Kbart : {\\\"PUBLICATION_TITLE\\\": null, \\\"PRINT_IDENTIFIER\\\": null, \\\"ONLINE_IDENTIFIER\\\": null, \\\"DATE_FIRST_ISSUE_ONLINE\\\": null, \\\"NUM_FIRST_VOL_ONLINE\\\": null, \\\"NUM_FIRST_ISSUE_ONLINE\\\": null, \\\"DATE_LAST_ISSUE_ONLINE\\\": null, \\\"NUM_LAST_VOL_ONLINE\\\": null, \\\"NUM_LAST_ISSUE_ONLINE\\\": null, \\\"TITLE_URL\\\": null, \\\"FIRST_AUTHOR\\\": null, \\\"TITLE_ID\\\": null, \\\"EMBARGO_INFO\\\": null, \\\"COVERAGE_DEPTH\\\": null, \\\"NOTES\\\": null, \\\"PUBLISHER_NAME\\\": null, \\\"PUBLICATION_TYPE\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_PRINT\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_ONLIN\\\": null, \\\"MONOGRAPH_VOLUME\\\": null, \\\"MONOGRAPH_EDITION\\\": null, \\\"FIRST_EDITOR\\\": null, \\\"PARENT_PUBLICATION_TITLE_ID\\\": null, \\\"PRECEDING_PUBLICATION_TITLE_ID\\\": null, \\\"ACCESS_TYPE\\\": null, \\\"PROVIDER_PACKAGE_PACKAGE\\\": null, \\\"PROVIDER_PACKAGE_DATE_P\\\": null, \\\"PROVIDER_PACKAGE_IDT_PROVIDER\\\": 0, \\\"ID_PROVIDER_PACKAGE\\\": 0, \\\"BEST_PPN\\\": null}, Notice : notice, Erreur : erreur], [PPN : ppn, Ligne Kbart : {\\\"PUBLICATION_TITLE\\\": null, \\\"PRINT_IDENTIFIER\\\": null, \\\"ONLINE_IDENTIFIER\\\": null, \\\"DATE_FIRST_ISSUE_ONLINE\\\": null, \\\"NUM_FIRST_VOL_ONLINE\\\": null, \\\"NUM_FIRST_ISSUE_ONLINE\\\": null, \\\"DATE_LAST_ISSUE_ONLINE\\\": null, \\\"NUM_LAST_VOL_ONLINE\\\": null, \\\"NUM_LAST_ISSUE_ONLINE\\\": null, \\\"TITLE_URL\\\": null, \\\"FIRST_AUTHOR\\\": null, \\\"TITLE_ID\\\": null, \\\"EMBARGO_INFO\\\": null, \\\"COVERAGE_DEPTH\\\": null, \\\"NOTES\\\": null, \\\"PUBLISHER_NAME\\\": null, \\\"PUBLICATION_TYPE\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_PRINT\\\": null, \\\"DATE_MONOGRAPH_PUBLISHED_ONLIN\\\": null, \\\"MONOGRAPH_VOLUME\\\": null, \\\"MONOGRAPH_EDITION\\\": null, \\\"FIRST_EDITOR\\\": null, \\\"PARENT_PUBLICATION_TITLE_ID\\\": null, \\\"PRECEDING_PUBLICATION_TITLE_ID\\\": null, \\\"ACCESS_TYPE\\\": null, \\\"PROVIDER_PACKAGE_PACKAGE\\\": null, \\\"PROVIDER_PACKAGE_DATE_P\\\": null, \\\"PROVIDER_PACKAGE_IDT_PROVIDER\\\": 0, \\\"ID_PROVIDER_PACKAGE\\\": 0, \\\"BEST_PPN\\\": null}, Notice : notice, Erreur : erreur]]\\r\\n\",\"2 erreur(s) lors de la création de notice(s) ExNihilo : [ExNihilo_1, ExNihilo_2]\\r\\n\",\"2 erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé : [Imprime_1, Imprime_1]\"]";

        WorkInProgress workInProgress = new WorkInProgress();

        workInProgress.addErrorMessagesConnectionCbs("connectionCBS_1");
        workInProgress.addErrorMessagesConnectionCbs("connectionCBS_2");

        workInProgress.addErrorMessagesDateFormat("Date_1");
        workInProgress.addErrorMessagesDateFormat("Date_2");

        workInProgress.addErrorMessagesAdd469WithoutNotice("ppn", new LigneKbartConnect(), "erreur");
        workInProgress.addErrorMessagesAdd469WithoutNotice("ppn", new LigneKbartConnect(), "erreur");

        workInProgress.addErrorMessagesDelete469("ppn", new LigneKbartConnect(), "notice", "erreur");
        workInProgress.addErrorMessagesDelete469("ppn", new LigneKbartConnect(), "notice", "erreur");

        workInProgress.addErrorMessageExNihilo("ExNihilo_1");
        workInProgress.addErrorMessageExNihilo("ExNihilo_2");

        workInProgress.addErrorMessagesImprime("Imprime_1");
        workInProgress.addErrorMessagesImprime("Imprime_1");

        Assertions.assertEquals(messageReference,workInProgress.getAllErrorMessages("FICHIER_TEST_TEST-SANS-OBJET_2024-01-01.tsv"));
    }

    @Test
    void isErrorFree() {
        WorkInProgress workInProgress = new WorkInProgress();
        Assertions.assertTrue(workInProgress.isErrorFree());

        WorkInProgress workInProgress1 = new WorkInProgress();
        workInProgress1.addErrorMessagesConnectionCbs("test");
        Assertions.assertFalse(workInProgress1.isErrorFree());

        WorkInProgress workInProgress2 = new WorkInProgress();
        workInProgress2.addErrorMessagesDateFormat("test");
        Assertions.assertFalse(workInProgress2.isErrorFree());

        WorkInProgress workInProgress3 = new WorkInProgress();
        workInProgress3.addErrorMessagesAdd469WithoutNotice("ppn", new LigneKbartConnect(), "erreur");
        Assertions.assertFalse(workInProgress3.isErrorFree());

        WorkInProgress workInProgress4 = new WorkInProgress();
        workInProgress4.addErrorMessagesDelete469("ppn", new LigneKbartConnect(), "notice", "erreur");
        Assertions.assertFalse(workInProgress4.isErrorFree());

        WorkInProgress workInProgress5 = new WorkInProgress();
        workInProgress5.addErrorMessageExNihilo("test");
        Assertions.assertFalse(workInProgress5.isErrorFree());

        WorkInProgress workInProgress6 = new WorkInProgress();
        workInProgress6.addErrorMessagesImprime("test");
        Assertions.assertFalse(workInProgress6.isErrorFree());
    }
}
