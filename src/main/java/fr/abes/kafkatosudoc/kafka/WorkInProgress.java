package fr.abes.kafkatosudoc.kafka;

import fr.abes.LigneKbartConnect;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class WorkInProgress {
    private final List<LigneKbartConnect> listeNotices;

    private Integer currentNbLines;

    private Integer nbLinesTotal;

    public WorkInProgress() {
        this.listeNotices = new ArrayList<>();
        this.currentNbLines = 0;
        this.nbLinesTotal = -1;
    }

    public void addNotice(LigneKbartConnect notice) {
        this.listeNotices.add(notice);
    }

    public void incrementCurrentNbLignes() {
        this.currentNbLines++;
    }
}
