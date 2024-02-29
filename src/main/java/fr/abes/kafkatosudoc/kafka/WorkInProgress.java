package fr.abes.kafkatosudoc.kafka;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
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

    private List<String> listErrorMessagesDelete469;

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

    public void addErrorMessagesAdd469(String message) {
        this.listErrorMessagesAdd469.add(message);
    }

    public void addErrorMessagesDelete469(String message) {
        this.listErrorMessagesDelete469.add(message);
    }

    public void addErrorMessageExNihilo(String message) { this.listErrorMessageExNihilo.add(message); }

    public void addErrorMessagesImprime(String message) { this.listErrorMessagesImprime.add(message); }

    public boolean isErrorFree() {
        return this.listErrorMessagesConnectionCbs.isEmpty() && this.listErrorMessagesDateFormat.isEmpty() && this.listErrorMessagesAdd469.isEmpty() && this.listErrorMessagesDelete469.isEmpty() && this.listErrorMessageExNihilo.isEmpty() && this.listErrorMessagesImprime.isEmpty();
    }

    public String getAllErrorMessages(String filename) {
        List<String> listErrors = new ArrayList<>();
        listErrors.add(filename + " " + System.lineSeparator());
        if (this.listErrorMessagesConnectionCbs != null && !this.listErrorMessagesConnectionCbs.isEmpty()) listErrors.add(this.listErrorMessagesConnectionCbs.size() + " erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets) : " + this.listErrorMessagesConnectionCbs + System.lineSeparator());
        if (this.listErrorMessagesDateFormat != null && !this.listErrorMessagesDateFormat.isEmpty()) listErrors.add(this.listErrorMessagesDateFormat.size() + " erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets) : " + this.listErrorMessagesDateFormat + System.lineSeparator());
        if (this.listErrorMessagesAdd469 != null && !this.listErrorMessagesAdd469.isEmpty()) listErrors.add(this.listErrorMessagesAdd469.size() + " erreur(s) d'ajout de 469 : " + this.listErrorMessagesAdd469 + System.lineSeparator());
        if (this.listErrorMessagesDelete469 != null && !this.listErrorMessagesDelete469.isEmpty()) listErrors.add(this.listErrorMessagesDelete469.size() + " erreur(s) de suppression de 469 : " + this.listErrorMessagesDelete469);
        if (this.listErrorMessageExNihilo != null && !this.listErrorMessageExNihilo.isEmpty()) listErrors.add(this.listErrorMessageExNihilo.size() + " erreur(s) lors de la création de notice(s) ExNihilo : " + this.listErrorMessageExNihilo);
        if (this.listErrorMessagesImprime != null && !this.listErrorMessagesImprime.isEmpty()) listErrors.add(this.listErrorMessagesImprime.size() + " erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé : " + this.listErrorMessagesImprime);
        return String.valueOf(listErrors);
    }
}
