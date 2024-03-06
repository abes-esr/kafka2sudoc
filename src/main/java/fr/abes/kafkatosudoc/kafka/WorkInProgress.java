package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void addErrorMessagesAdd469WithNotice(String ppn, LigneKbartConnect ligneKbart, String notice, String erreur) {
        // TODO trouver comment convertir une ligneKbart en json exploitable (trop de \ ) (peut-être remapper l'objet via une méthode faite main)
        JsonObject erreurToAdd = Json.createObjectBuilder()
                .add("PPN", ppn)
                .add("Ligne Kbart", ligneKbart.toString())
                .add("Notice", notice)
                .add("Erreur", erreur)
                .build();
        this.listErrorMessagesAdd469.add(erreurToAdd.toString());
    }

    public void addErrorMessagesAdd469WithoutNotice(String ppn, LigneKbartConnect ligneKbart, String erreur) {
        // TODO trouver comment convertir une ligneKbart en json exploitable (trop de \ ) (peut-être remapper l'objet via une méthode faite main)
        JsonObject erreurToAdd = Json.createObjectBuilder()
                .add("PPN", ppn)
                .add("Ligne Kbart", ligneKbart.toString())
                .add("Erreur", erreur)
                .build();
        this.listErrorMessagesAdd469.add(erreurToAdd.toString());
    }

    public void addErrorMessagesDelete469(String ppn, LigneKbartConnect ligneKbart, String notice, String erreur) {
        // TODO trouver comment convertir une ligneKbart en json exploitable (trop de \ ) (peut-être remapper l'objet via une méthode faite main)
        JsonObject erreurToAdd = Json.createObjectBuilder()
            .add("PPN : ", ppn)
            .add("Ligne Kbart : ", ligneKbart.toString())
            .add("Notice : ", notice)
            .add("Erreur : ", erreur)
            .build();
        this.listErrorMessagesDelete469.add(erreurToAdd.toString());
    }

    public void addErrorMessageExNihilo(String ppn, String erreur) {
        JsonObject erreurToAdd = Json.createObjectBuilder()
            .add("PPN : ", ppn)
            .add("Erreur : ", erreur)
            .build();
        this.listErrorMessageExNihilo.add(erreurToAdd.toString());
    }

    public void addErrorMessagesImprime(String ppn, String notice, String erreur) {
        JsonObject erreurToAdd = Json.createObjectBuilder()
            .add("PPN : ", ppn)
            .add("Notice : ", notice)
            .add("Erreur : ", erreur)
            .build();
        this.listErrorMessagesImprime.add(erreurToAdd.toString());
    }

    public boolean isErrorFree() {
        return this.listErrorMessagesConnectionCbs.isEmpty() && this.listErrorMessagesDateFormat.isEmpty() && this.listErrorMessagesAdd469.isEmpty() && this.listErrorMessagesDelete469.isEmpty() && this.listErrorMessageExNihilo.isEmpty() && this.listErrorMessagesImprime.isEmpty();
    }

    public String getAllErrorMessages(String filename) throws JsonProcessingException {
        String provider = CheckFiles.getProviderFromFilename(filename);
        String packageName = CheckFiles.getPackageFromFilename(filename);
        String date = "";

        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(filename);
        if(matcher.find()){
            date = matcher.group(1);
        }

        JsonObject listErrors = Json.createObjectBuilder()
                        .add("Provider", provider)
                        .add("Package", packageName)
                        .add("Date", date)
                        .add(this.listErrorMessagesConnectionCbs.size() + " erreur(s) de connection CBS lors d'une mise à jour des zones 469 de liens vers les notices bouquets) : ", this.listErrorMessagesConnectionCbs.toString())
                        .add(this.listErrorMessagesDateFormat.size() + " erreur(s) de format de date lors d'une mise à jour des zones 469 ou de liens vers les notices bouquets) : ", this.listErrorMessagesDateFormat.toString())
                        .add(this.listErrorMessagesAdd469.size() + " erreur(s) d'ajout de 469 : ", this.listErrorMessagesAdd469.toString())
                        .add(this.listErrorMessagesDelete469.size() + " erreur(s) de suppression de 469 : ", this.listErrorMessagesDelete469.toString())
                        .add(this.listErrorMessageExNihilo.size() + " erreur(s) lors de la création de notice(s) ExNihilo : ", this.listErrorMessageExNihilo.toString())
                        .add(this.listErrorMessagesImprime.size() + " erreur(s) lors de la création de notice(s) électronique(s) à partir d'un imprimé : ", this.listErrorMessagesImprime.toString())
                        .build();

        return listErrors.toString();
    }
}
