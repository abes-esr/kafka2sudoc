package fr.abes.kafkatosudoc.dto;

import fr.abes.LigneKbartImprime;
import fr.abes.cbs.notices.NoticeConcrete;
import lombok.Data;

@Data
public class KbartAndImprimeDto {
    private LigneKbartImprime kbart;
    private NoticeConcrete notice;
}
