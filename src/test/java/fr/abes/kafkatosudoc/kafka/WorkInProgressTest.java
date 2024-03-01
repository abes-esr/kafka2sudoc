package fr.abes.kafkatosudoc.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkInProgressTest {

    @Test
    void getAllErrorMessages() {
        String messageReference = "[fichier_test " + System.lineSeparator() +
                ", 2 erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets) : [connectionCBS_1, connectionCBS_2]" + System.lineSeparator() +
                ", 2 erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets) : [Date_1, Date_2]" + System.lineSeparator() +
                ", 2 erreur(s) d'ajout de 469 : [Add_469_1, Add_469_2]" + System.lineSeparator() +
                ", 2 erreur(s) de suppression de 469 : [Del_469_1, Del_469_2]" + System.lineSeparator() +
                ", 2 erreur(s) lors de la création de notice(s) ExNihilo : [ExNihilo_1, ExNihilo_2]" + System.lineSeparator() +
                ", 2 erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé : [Imprime_1, Imprime_1]]";

        WorkInProgress workInProgress = new WorkInProgress();

        workInProgress.addErrorMessagesConnectionCbs("connectionCBS_1");
        workInProgress.addErrorMessagesConnectionCbs("connectionCBS_2");

        workInProgress.addErrorMessagesDateFormat("Date_1");
        workInProgress.addErrorMessagesDateFormat("Date_2");

        workInProgress.addErrorMessagesAdd469("Add_469_1");
        workInProgress.addErrorMessagesAdd469("Add_469_2");

        workInProgress.addErrorMessagesDelete469("Del_469_1");
        workInProgress.addErrorMessagesDelete469("Del_469_2");

        workInProgress.addErrorMessageExNihilo("ExNihilo_1");
        workInProgress.addErrorMessageExNihilo("ExNihilo_2");

        workInProgress.addErrorMessagesImprime("Imprime_1");
        workInProgress.addErrorMessagesImprime("Imprime_1");

        Assertions.assertEquals(messageReference,workInProgress.getAllErrorMessages("fichier_test"));
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
        workInProgress3.addErrorMessagesAdd469("test");
        Assertions.assertFalse(workInProgress3.isErrorFree());

        WorkInProgress workInProgress4 = new WorkInProgress();
        workInProgress4.addErrorMessagesDelete469("test");
        Assertions.assertFalse(workInProgress4.isErrorFree());

        WorkInProgress workInProgress5 = new WorkInProgress();
        workInProgress5.addErrorMessageExNihilo("test");
        Assertions.assertFalse(workInProgress5.isErrorFree());

        WorkInProgress workInProgress6 = new WorkInProgress();
        workInProgress6.addErrorMessagesImprime("test");
        Assertions.assertFalse(workInProgress6.isErrorFree());
    }
}
