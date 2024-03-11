package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.abes.LigneKbartConnect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkInProgressTest {

    @Test
    void isErrorFree() throws JsonProcessingException {
        WorkInProgress workInProgress = new WorkInProgress();
        Assertions.assertTrue(workInProgress.isCreateFromKbartErrorFree());

        WorkInProgress workInProgress1 = new WorkInProgress();
        Assertions.assertTrue(workInProgress1.isFromKafkaExNihiloErrorFree());

        WorkInProgress workInProgress2 = new WorkInProgress();
        Assertions.assertTrue(workInProgress2.isFromKafkaToImprimeErrorFree());

        WorkInProgress workInProgress3 = new WorkInProgress();
        workInProgress3.addErrorMessagesConnectionCbs("test");
        Assertions.assertFalse(workInProgress3.isCreateFromKbartErrorFree());

        WorkInProgress workInProgress4 = new WorkInProgress();
        workInProgress4.addErrorMessagesDateFormat("test");
        Assertions.assertFalse(workInProgress4.isCreateFromKbartErrorFree());

        WorkInProgress workInProgress6 = new WorkInProgress();
        workInProgress6.addErrorMessagesDelete469("ppn", new LigneKbartConnect(), "notice", "erreur");
        Assertions.assertFalse(workInProgress6.isCreateFromKbartErrorFree());

        WorkInProgress workInProgress7 = new WorkInProgress();
        workInProgress7.addErrorMessageExNihilo("ppn", "erreur");
        Assertions.assertFalse(workInProgress7.isFromKafkaExNihiloErrorFree());

        WorkInProgress workInProgress8 = new WorkInProgress();
        workInProgress8.addErrorMessagesImprime("ppn", "notice", "erreur");
        Assertions.assertFalse(workInProgress8.isFromKafkaToImprimeErrorFree());
    }
}
