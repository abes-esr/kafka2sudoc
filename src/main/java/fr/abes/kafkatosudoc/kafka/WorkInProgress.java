package fr.abes.kafkatosudoc.kafka;

import fr.abes.LigneKbartConnect;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class WorkInProgress<T> {
    private final List<T> listeNotices;

    private AtomicInteger currentNbLines;

    private Integer nbLinesTotal;

    private List<ErrorMessage> errorMessages;

    public WorkInProgress(int nbLinesTotal) {
        this.listeNotices = Collections.synchronizedList(new ArrayList<>());
        this.currentNbLines = new AtomicInteger(0);
        this.nbLinesTotal = nbLinesTotal;
        this.errorMessages = Collections.synchronizedList(new ArrayList<>());
    }

    public void addNotice(T notice) {
        this.listeNotices.add(notice);
    }

    public void incrementCurrentNbLignes() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StackTraceElement caller = stack[1];
        log.debug("Méthode " + caller.getMethodName() + " | Current line : " + this.currentNbLines.incrementAndGet() + " | total lines : " + this.getNbLinesTotal());
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
                .add("Erreur", erreur)
                .add("Ligne Kbart", ligneKbart.toString().replace("\"", ""))
                .add("Notice", notice)
                .build();
        this.errorMessages.add(new ErrorMessage(errortype, erreurToAdd.toString().replace("\"", "")));
    }

    public void addErrorMessageExNihilo(String ppn, String erreur) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.EXNIHILO, "{Ppn : " + ppn + ", Erreur : " + erreur + "}"));

    }

    public void addErrorMessagesImprime(String ppn, String notice, String erreur) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.FROMIMPRIME, "{Ppn : " + ppn + ", Erreur : " + erreur +", Notice : " + notice + "}"));
    }

    public void addErrorMessagesImprime(String ppn,String erreur) {
        this.errorMessages.add(new ErrorMessage(ERROR_TYPE.FROMIMPRIME, "{Ppn : " + ppn + ", Erreur : " + erreur + "}"));
    }

}
