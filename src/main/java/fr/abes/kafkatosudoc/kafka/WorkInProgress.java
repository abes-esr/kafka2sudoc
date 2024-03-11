package fr.abes.kafkatosudoc.kafka;

import fr.abes.LigneKbartConnect;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WorkInProgress<T> {
    private final List<T> listeNotices;

    private Integer currentNbLines;

    private Integer nbLinesTotal;

    private List<ErrorMessage> errorMessages;

    public WorkInProgress() {
        this.listeNotices = new ArrayList<>();
        this.currentNbLines = 0;
        this.nbLinesTotal = -1;
        this.errorMessages = new ArrayList<>();
    }

    public void addNotice(T notice) {
        this.listeNotices.add(notice);
    }

    public void incrementCurrentNbLignes() {
        this.currentNbLines++;
    }

    public void addErrorMessagesConnectionCbs(String message) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.CONNEXION, message));
    }

    public void addErrorMessagesDateFormat(String message) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.DATE_FORMAT, message));
    }

    public void addErrorMessages469(String ppn, LigneKbartConnect ligneKbart, String notice, String erreur, ERROR_TYPE errortype) {
        JsonObject erreurToAdd = Json.createObjectBuilder()
                .add("PPN", ppn)
                .add("Ligne Kbart", ligneKbart.toString().replace("\"", ""))
                .add("Notice", notice)
                .add("Erreur", erreur)
                .build();
        this.errorMessages.add(new ErrorMessage(errortype, erreurToAdd.toString().replace("\"", "")));
    }

    public void addErrorMessageExNihilo(String ppn, String erreur) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.EXNIHILO, "{ppn : " + ppn + ", erreur : " + erreur + "}"));

    }

    public void addErrorMessagesImprime(String ppn, String notice, String erreur) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.FROMIMPRIME, "{Ppn : " + ppn + ", Notice : " + notice + ", Erreur : " + erreur + "}"));
    }

}
