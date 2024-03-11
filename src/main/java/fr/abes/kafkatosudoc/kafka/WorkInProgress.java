package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WorkInProgress {
    private final List<LigneKbartConnect> listeNotices;

    private final List<LigneKbartImprime> listeNoticesImprime;

    private Integer currentNbLines;

    private Integer nbLinesTotal;

    private List<String> listErrorMessagesConnectionCbs;

    private List<String> listErrorMessagesDateFormat;

    private List<String> listErrorMessagesAdd469;

    private List<JsonObject> listErrorMessagesDelete469;
//    private List<String> listErrorMessagesDelete469;

    private List<String> listErrorMessageExNihilo;

    private List<String> listErrorMessagesImprime;

    public WorkInProgress() {
        this.listeNotices = new ArrayList<>();
        this.listeNoticesImprime = new ArrayList<>();
        this.currentNbLines = 0;
        this.nbLinesTotal = -1;
        this.listErrorMessagesConnectionCbs = new ArrayList<>();
        this.listErrorMessagesDateFormat = new ArrayList<>();
        this.listErrorMessagesAdd469 = new ArrayList<>();
        this.listErrorMessagesDelete469 = new ArrayList<>();
        this.listErrorMessageExNihilo = new ArrayList<>();
        this.listErrorMessagesImprime = new ArrayList<>();
    }

    public void addNotice(LigneKbartConnect notice) {
        this.listeNotices.add(notice);
    }

    public void addNoticeImprime(LigneKbartImprime notice) { this.listeNoticesImprime.add(notice); }

    public void incrementCurrentNbLignes() {
        this.currentNbLines++;
    }

    public void addErrorMessagesConnectionCbs(String message) {
        this.listErrorMessagesConnectionCbs.add(message);
    }

    public void addErrorMessagesDateFormat(String message) {
        this.listErrorMessagesDateFormat.add(message);
    }

    public void addErrorMessagesAdd469WithNotice(String ppn, LigneKbartConnect ligneKbart, String notice, String erreur) {
        this.listErrorMessagesAdd469.add("{Ppn : " + ppn + ", Ligne Kbart : " + ligneKbart.toString() + ", Notice : " + notice + ", Erreur : " + erreur + "}");
    }

    public void addErrorMessagesDelete469(String ppn, LigneKbartConnect ligneKbart, String notice, String erreur) {
        JsonObject errorMessage = Json.createObjectBuilder()
                .add("Ppn", ppn)
                .add("Ligne Kbart", ligneKbart.toString())
                .add("Notice", notice)
                .add("Erreur", erreur)
                .build();
        this.listErrorMessagesDelete469.add(errorMessage);
//        this.listErrorMessagesDelete469.add("{Ppn : " + ppn + ", Ligne Kbart : " + ligneKbart.toString() + ", Notice : " + notice + ", Erreur : " + erreur + "}");
    }

    public void addErrorMessageExNihilo(String ppn, String erreur) throws JsonProcessingException {
        this.listErrorMessageExNihilo.add("{ppn : " + ppn + ", erreur : " + erreur + "}");

    }

    public void addErrorMessagesImprime(String ppn, String notice, String erreur) {
        this.listErrorMessagesImprime.add("{Ppn : " + ppn + ", Notice : " + notice + ", Erreur : " + erreur + "}");
    }

    public boolean isCreateFromKbartErrorFree() {
        return this.listErrorMessagesConnectionCbs.isEmpty() && this.listErrorMessagesDateFormat.isEmpty() && this.listErrorMessagesAdd469.isEmpty() && this.listErrorMessagesDelete469.isEmpty();
    }

    public boolean isFromKafkaExNihiloErrorFree() {
        return this.listErrorMessageExNihilo.isEmpty();
    }

    public boolean isFromKafkaToImprimeErrorFree() {
        return this.listErrorMessagesImprime.isEmpty();
    }
}
