package fr.abes.kafkatosudoc.utils;

import com.google.common.collect.Table;
import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.*;
import fr.abes.kafkatosudoc.dto.KbartAndImprimeDto;
import fr.abes.kafkatosudoc.entity.LigneKbart;
import lombok.SneakyThrows;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class NoticeMapper {
    private final UtilsMapper mapper;

    public NoticeMapper(UtilsMapper mapper) {
        this.mapper = mapper;
    }

    @Bean
    public void converterKbartToNoticeConcrete() {
        Converter<LigneKbartConnect, NoticeConcrete> myConverter = new Converter<LigneKbartConnect, NoticeConcrete>() {
            @SneakyThrows
            public NoticeConcrete convert(MappingContext<LigneKbartConnect, NoticeConcrete> context) {
                LigneKbartConnect kbart = context.getSource();
                //création de la notice biblio
                Biblio noticeBiblio = new Biblio();
                //ajout zone type de document
                noticeBiblio.addZone("008", "$a", "Oax3");
                //ajout ISBN
                if (!kbart.getONLINEIDENTIFIER().isEmpty()) {
                    if ((Utils.getIsbnType(kbart.getONLINEIDENTIFIER().toString()).equals(ISBN_TYPE.ISBN10))) {
                        noticeBiblio.addZone("010", "$a", Utils.addHyphensToIsbn(kbart.getONLINEIDENTIFIER().toString()));
                    } else {
                        noticeBiblio.addZone("010", "$A", Utils.addHyphensToIsbn(kbart.getONLINEIDENTIFIER().toString()));
                    }
                }

                //DOI
                String doi = Utils.extractDoiFromConnect(kbart);
                if (!doi.isEmpty()) {
                    noticeBiblio.addZone("017", "$a", doi, new char[]{'7', '0'});
                    noticeBiblio.addSousZone("017", "$2", "DOI");
                }
                //Date de publication
                if (kbart.getDATEMONOGRAPHPUBLISHEDONLIN() != null)
                    noticeBiblio.addZone("100", "$a", Utils.getYearFromDate(kbart.getDATEMONOGRAPHPUBLISHEDONLIN().toString()), new char[]{'0', '#'});
                else
                    noticeBiblio.addZone("100", "$a", "20XX", new char[]{'0', '#'});

                //Langue de publication
                noticeBiblio.addZone("101", "$a", "und", new char[]{'#', '#'});
                //Pays de publication
                noticeBiblio.addZone("102", "$a", "XX");

                //Données codées ressources électroniques
                noticeBiblio.addZone("135", "$b", "r");
                noticeBiblio.addZone("181", "$P", "01");
                noticeBiblio.addSousZone("181", "$c", "txt");
                //Type de contenu / médiation
                noticeBiblio.addZone("182", "$P", "01");
                noticeBiblio.addSousZone("182", "$c", "c");
                noticeBiblio.addZone("183", "$P", "01");
                noticeBiblio.addSousZone("183", "$a", "ceb");
                //Titre

                noticeBiblio.addZone("200", "$a", "@" + kbart.getPUBLICATIONTITLE(), new char[]{'1', '#'});
                if (!kbart.getFIRSTAUTHOR().isEmpty())
                    noticeBiblio.addSousZone("200", "$f", kbart.getFIRSTAUTHOR().toString());
                //Mention de publication / diffusion
                Zone zone214diese0 = new Zone("214", TYPE_NOTICE.BIBLIOGRAPHIQUE, new char[]{'#', '0'});
                zone214diese0.addSubLabel("$a", "[Lieu de publication inconnu]");
                if (kbart.getPUBLISHERNAME() != null)
                    zone214diese0.addSubLabel("$c", kbart.getPUBLISHERNAME().toString());
                noticeBiblio.addZone(zone214diese0);

                Zone zone214diese2 = new Zone("214", TYPE_NOTICE.BIBLIOGRAPHIQUE, new char[]{'#', '2'});
                zone214diese2.addSubLabel("$a", "[Lieu de diffusion inconnu]");
                if (kbart.getDATEMONOGRAPHPUBLISHEDONLIN() != null && !kbart.getDATEMONOGRAPHPUBLISHEDONLIN().toString().isEmpty()) {
                    zone214diese2.addSubLabel("$d", Utils.getYearFromDate(kbart.getDATEMONOGRAPHPUBLISHEDONLIN().toString()));
                } else {
                    zone214diese2.addSubLabel("$d", "[20..]");
                }
                noticeBiblio.addZone(zone214diese2);
                noticeBiblio.addZone("309", "$a", "Notice générée automatiquement à partir des métadonnées de BACON. SUPPRIMER LA PRESENTE NOTE 309 APRES MISE A JOUR");

                //Note sur les conditions d'accès
                if (kbart.getACCESSTYPE().toString().equals("F"))
                    noticeBiblio.addZone("371", "$a", "Ressource en accès libre", new char[]{'0', '#'});
                else
                    noticeBiblio.addZone("371", "$a", "Accès en ligne réservé aux établissements ou bibliothèques ayant souscrit l'abonnement", new char[]{'0', '#'});

                //1er auteur
                if (!kbart.getFIRSTAUTHOR().isEmpty()) {
                    noticeBiblio.addZone("700", "$a", kbart.getFIRSTAUTHOR().toString(), new char[]{'#', '1'});
                    noticeBiblio.addSousZone("700", "$4", "070");
                }

                //editeur
                if (!kbart.getFIRSTEDITOR().isEmpty()) {
                    if (kbart.getFIRSTAUTHOR().isEmpty()) {
                        noticeBiblio.addZone("700", "$a", kbart.getFIRSTEDITOR().toString(), new char[]{'#', '1'});
                        noticeBiblio.addSousZone("700", "$4", "651");
                    } else {
                        noticeBiblio.addZone("701", "$a", kbart.getFIRSTEDITOR().toString(), new char[]{'#', '1'});
                        noticeBiblio.addSousZone("701", "$4", "651");
                    }

                }

                //url d'accès
                if (!kbart.getTITLEURL().isEmpty()) {
                    if (kbart.getACCESSTYPE().toString().equals("F")) {
                        noticeBiblio.addZone("856", "$u", kbart.getTITLEURL().toString(), new char[]{'4', '#'});
                    } else {
                        noticeBiblio.addZone("859", "$u", kbart.getTITLEURL().toString(), new char[]{'4', '#'});
                    }
                }
                return new NoticeConcrete(noticeBiblio);
            }
        };
        mapper.addConverter(myConverter);
    }

    @Bean
    public void converterKbartAndImprimeToNoticeConcrete() {
        Converter<KbartAndImprimeDto, NoticeConcrete> myConverter = new Converter<KbartAndImprimeDto, NoticeConcrete>() {
            @SneakyThrows
            public NoticeConcrete convert(MappingContext<KbartAndImprimeDto, NoticeConcrete> context) {
                LigneKbartImprime kbart = context.getSource().getKbart();
                NoticeConcrete noticeImprimee = context.getSource().getNotice();

                Biblio noticeElec = new Biblio();
                noticeElec.addZone("008", "$a", "Oax3");
                if (kbart.getOnlineIdentifier() != null && !kbart.getOnlineIdentifier().isEmpty()) {
                    if ((Utils.getIsbnType(kbart.getOnlineIdentifier().toString()).equals(ISBN_TYPE.ISBN10))) {
                        noticeElec.addZone("010", "$a", Utils.addHyphensToIsbn(kbart.getOnlineIdentifier().toString()));
                    } else {
                        noticeElec.addZone("010", "$A", Utils.addHyphensToIsbn(kbart.getOnlineIdentifier().toString()));
                    }
                }

                //DOI
                String doi = Utils.extractDoiFromImprime(kbart);
                if (!doi.isEmpty()) {
                    noticeElec.addZone("017", "$a", doi, new char[]{'7', '0'});
                    noticeElec.addSousZone("017", "$2", "DOI");
                }

                //Date de publication
                if (kbart.getDateMonographPublishedOnline() != null && !kbart.getDateMonographPublishedOnline().isEmpty())
                    noticeElec.addZone("100", "$a", Utils.getYearFromDate(kbart.getDateMonographPublishedOnline().toString()), new char[]{'0', '#'});
                else
                    noticeElec.addZone("100", "$a", "20XX", new char[]{'0', '#'});

                //langue de publication
                Zone zone101 = noticeImprimee.getNoticeBiblio().findZone("101", 0);
                noticeElec.addZone(zone101);
                //Pays de publication
                noticeElec.addZone("102", "$a", "XX");
                //Données générales de traitement
                Zone zone104Imprime = noticeImprimee.getNoticeBiblio().findZone("104", 0);
                if (zone104Imprime != null) {
                    noticeElec.addZone(zone104Imprime);
                }
                //Données codées monographie
                Zone zone105Imprime = noticeImprimee.getNoticeBiblio().findZone("105", 0);
                if (zone105Imprime != null) {
                    noticeElec.addZone(zone105Imprime);
                    if (zone105Imprime.findSubLabel("$b") != null && zone105Imprime.findSubLabel("$b").equals("m")) {
                        noticeElec.replaceSousZone("105", "$b", "v");
                    }
                }

                //Données codées ressources électroniques
                noticeElec.addZone("135", "$b", "r");
                noticeElec.addZone("181", "$P", "01");
                noticeElec.addSousZone("181", "$c", "txt");
                //Type de contenu / médiation
                noticeElec.addZone("182", "$P", "01");
                noticeElec.addSousZone("182", "$c", "c");
                noticeElec.addZone("183", "$P", "01");
                noticeElec.addSousZone("183", "$a", "ceb");

                //titre
                noticeElec.addZone(noticeImprimee.getNoticeBiblio().findZone("200", 0));
                //Mention d'édition
                Zone zone205 = noticeImprimee.getNoticeBiblio().findZone("205", 0);
                if (zone205 != null) {
                    noticeElec.addZone(zone205);
                }

                //Mention  de publication
                List<Zone> listZone214 = noticeImprimee.getNoticeBiblio().findZones("214").stream().filter(zone -> Arrays.toString(zone.getIndicateurs()).equals("[#, 0]")).toList();
                if (listZone214.isEmpty()) {
                    noticeElec.addZone("214", "$a", "[Lieu de publication inconnu]", new char[]{'#', '0'});
                    if (kbart.getPublisherName() != null)
                        noticeElec.addSousZone("214", "$c", kbart.getPublisherName().toString());
                }

                for (Zone zone214 : listZone214) {
                    Zone zone214Elec = new Zone("214", TYPE_NOTICE.BIBLIOGRAPHIQUE, zone214.getIndicateurs());
                    zone214.getSubLabelTable().rowMap().values().forEach(sousZones -> {
                        for (Map.Entry<String, String> entry : sousZones.entrySet()) {
                            String label = entry.getKey();
                            String value = entry.getValue();
                            if (label.equals("$a") || label.equals("$c")) {
                                try {
                                    zone214Elec.addSubLabel(label, value);
                                } catch (ZoneException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
                    if (zone214Elec.findSubLabel("$a") == null) {
                        zone214Elec.addSubLabel("$a", "[Lieu de publication inconnu]");
                    }
                    if (zone214Elec.findSubLabel("$c") == null) {
                        zone214Elec.addSubLabel("$c", kbart.getPublisherName().toString());
                    }
                    noticeElec.addZone(zone214Elec);
                }

                Zone zone214 = new Zone("214", TYPE_NOTICE.BIBLIOGRAPHIQUE, new char[]{'#', '2'});
                zone214.addSubLabel("$a", "[Lieu de diffusion inconnu]");
                if (kbart.getDateMonographPublishedOnline() != null && !kbart.getDateMonographPublishedOnline().isEmpty()) {
                    zone214.addSubLabel("$d", kbart.getDateMonographPublishedOnline().toString());
                } else {
                    zone214.addSubLabel("$d", "[20..]");
                }
                noticeElec.addZone(zone214);

                //gestion 210 résiduelles
                List<Zone> zones210 = noticeImprimee.getNoticeBiblio().findZones("210");
                for (Zone zone210 : zones210) {
                    zone214 = new Zone("214", TYPE_NOTICE.BIBLIOGRAPHIQUE, new char[]{'#', '0'});
                    String ssZone210a = zone210.findSubLabel("$a");
                    zone214.addSubLabel("$a", Objects.requireNonNullElse(ssZone210a, "[Lieu de publication inconnu]"));
                    String ssZone210c = zone210.findSubLabel("$c");
                    if (ssZone210c != null) {
                        zone214.addSubLabel("$c", ssZone210c);
                    } else {
                        if (kbart.getPublisherName() != null)
                            zone214.addSubLabel("$c", kbart.getPublisherName().toString());
                    }
                    noticeElec.addZone(zone214);
                }
                Zone zone328 = noticeImprimee.getNoticeBiblio().findZone("328", 0);
                if (zone328 != null) {
                    noticeElec.addZone(zone328);
                    if (noticeElec.findZone("328", 0).findSubLabel("$z") != null)
                        noticeElec.replaceSousZone("328", "$z", "Autre édition de");
                    else
                        noticeElec.addSousZone("328", "$z", "Autre édition de");
                }

                //Note sur les conditions d'accès
                if (kbart.getAccessType().toString().equals("F"))
                    noticeElec.addZone("371", "$a", "Ressource en accès libre", new char[]{'0', '#'});
                else
                    noticeElec.addZone("371", "$a", "Accès en ligne réservé aux établissements ou bibliothèques ayant souscrit l'abonnement", new char[]{'0', '#'});

                noticeElec.addZone("452", "$5", kbart.getPpn().toString(), new char[]{'#', '#'});

                Zone zone454 = noticeImprimee.getNoticeBiblio().findZone("454", 0);
                if (zone454 != null && zone454.findSubLabel("$0") == null) {
                    String zone454dollart = zone454.findSubLabel("$t");
                    if (zone454dollart != null)
                        noticeElec.addZone("454", "$t", zone454dollart, new char[]{'#', '#'});
                }
                //zone 5XX sauf 579, 512 et 516
                List<Zone> zones500 = noticeImprimee.getNoticeBiblio().getListeZones().values().stream().filter(zone -> zone.getLabel().startsWith("5")).filter(zone -> (!zone.getLabel().equals("579") && !zone.getLabel().equals("512") && !zone.getLabel().equals("516"))).toList();
                for (Zone zone1 : zones500) {
                    replaceSublabel3With5(noticeElec, zone1);
                }

                List<Zone> zones600 = noticeImprimee.getNoticeBiblio().getListeZones().values().stream().filter(zone -> zone.getLabel().startsWith("6")).toList();

                for (Zone zone1 : zones600) {
                    Zone zoneACreer = new Zone(zone1.getLabel(), zone1.getTypeNotice(), zone1.getIndicateurs());
                    Table<Integer, String, String> ssZones = zone1.getSubLabelTable();
                    for (int i = 0; i < ssZones.rowKeySet().size(); i++) {
                        ssZones.row(i).forEach((key, valeur) -> {
                            if (key.equals("$3")) {
                                try {
                                    zoneACreer.addSubLabel("$5", Utils.deleteExpensionFromValue(valeur));
                                } catch (ZoneException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                try {
                                    zoneACreer.addSubLabel(key, Utils.deleteExpensionFromValue(valeur));
                                } catch (ZoneException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }
                    noticeElec.addZone(zoneACreer);
                }

                for (Zone zone700 : noticeImprimee.getNoticeBiblio().findZones("700")) {
                    replaceSublabel3With5(noticeElec, zone700);
                }
                for (Zone zone701 : noticeImprimee.getNoticeBiblio().findZones("701")) {
                    replaceSublabel3With5(noticeElec, zone701);
                }

                for (Zone zone710 : noticeImprimee.getNoticeBiblio().findZones("710")) {
                    replaceSublabel3With5(noticeElec, zone710);
                }
                for (Zone zone711 : noticeImprimee.getNoticeBiblio().findZones("711")) {
                    replaceSublabel3With5(noticeElec, zone711);
                }

                //url d'accès
                if (kbart.getTitleUrl() != null)
                    if (kbart.getAccessType().toString().equals("F"))
                        noticeElec.addZone("856", "$u", kbart.getTitleUrl().toString(), new char[]{'4', '#'});
                    else
                        noticeElec.addZone("859", "$u", kbart.getTitleUrl().toString(), new char[]{'4', '#'});

                return new NoticeConcrete(noticeElec);
            }
        };
        mapper.addConverter(myConverter);
    }

    private void replaceSublabel3With5(Biblio noticeElec, Zone zone) throws ZoneException {
        if (zone.findSubLabel("$3") != null) {
            Zone newZone = new Zone(zone.getLabel(), zone.getTypeNotice(), zone.getIndicateurs());
            newZone.addSubLabel("$5", zone.findSubLabel("$3").substring(0, 9));
            zone.deleteSubLabel("$3");
            for (Map.Entry<String, String> entry : zone.getSubLabelTable().rowMap().values().stream().flatMap(map -> map.entrySet().stream()).toList()) {
                try {
                    newZone.addSubLabel(entry.getKey(), entry.getValue());
                } catch (ZoneException e) {
                    throw new RuntimeException(e);
                }
            }
            noticeElec.addZone(newZone);
        } else {
            noticeElec.addZone(zone);
        }
    }

    @Bean
    public void converterLigneKbartToLigneKbartConnect() {
        Converter<LigneKbart, LigneKbartConnect> myConverter = new Converter<LigneKbart, LigneKbartConnect>() {
            public LigneKbartConnect convert(MappingContext<LigneKbart, LigneKbartConnect> context) {
                LigneKbart ligneKbart = context.getSource();
                DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                return LigneKbartConnect.newBuilder()
                        .setCURRENTLINE(-1)
                        .setTOTALLINES(-1)
                        .setPUBLICATIONTITLE(ligneKbart.getPublicationTitle())
                        .setPUBLICATIONTYPE(ligneKbart.getPublicationType())
                        .setPRINTIDENTIFIER(ligneKbart.getPrintIdentifier())
                        .setONLINEIDENTIFIER(ligneKbart.getOnlineIdentifer())
                        .setDATEFIRSTISSUEONLINE(ligneKbart.getDateFirstIssueOnline() != null ? format.format(ligneKbart.getDateFirstIssueOnline()) : null)
                        .setNUMFIRSTVOLONLINE(ligneKbart.getNumFirstVolOnline())
                        .setNUMFIRSTISSUEONLINE(ligneKbart.getNumFirstIssueOnline())
                        .setDATELASTISSUEONLINE(ligneKbart.getDateLastIssueOnline() != null ? format.format(ligneKbart.getDateLastIssueOnline()) : null)
                        .setNUMLASTVOLONLINE(ligneKbart.getNumLastVolOnline())
                        .setNUMLASTISSUEONLINE(ligneKbart.getNumlastIssueOnline())
                        .setTITLEURL(ligneKbart.getTitleUrl())
                        .setFIRSTAUTHOR(ligneKbart.getFirstAuthor())
                        .setTITLEID(ligneKbart.getTitleId())
                        .setEMBARGOINFO(ligneKbart.getEmbargoInfo())
                        .setCOVERAGEDEPTH(ligneKbart.getCoverageDepth())
                        .setNOTES(ligneKbart.getNotes())
                        .setPUBLISHERNAME(ligneKbart.getPublisherName())
                        .setPUBLICATIONTYPE(ligneKbart.getPublicationType())
                        .setDATEMONOGRAPHPUBLISHEDPRINT(ligneKbart.getDateMonographPublishedPrint() != null ? format.format(ligneKbart.getDateMonographPublishedPrint()) : null)
                        .setDATEMONOGRAPHPUBLISHEDONLIN(ligneKbart.getDateMonographPublishedOnline() != null ? format.format(ligneKbart.getDateMonographPublishedOnline()) : null)
                        .setMONOGRAPHVOLUME(ligneKbart.getMonographVolume())
                        .setMONOGRAPHEDITION(ligneKbart.getMonographEdition())
                        .setFIRSTEDITOR(ligneKbart.getFirstEditor())
                        .setPARENTPUBLICATIONTITLEID(ligneKbart.getParentPublicationTitleId())
                        .setPRECEDINGPUBLICATIONTITLEID(ligneKbart.getPrecedeingPublicationTitleId())
                        .setACCESSTYPE(ligneKbart.getAccessType())
                        .setPROVIDERPACKAGEPACKAGE(ligneKbart.getProviderPackage().getPackageName())
                        .setPROVIDERPACKAGEDATEP(ligneKbart.getProviderPackage().getDateP().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                        .setPROVIDERPACKAGEIDTPROVIDER(ligneKbart.getProviderPackage().getProvider().getIdtProvider())
                        .setIDPROVIDERPACKAGE(ligneKbart.getProviderPackage().getProviderPackageId())
                        .setBESTPPN(ligneKbart.getBestPpn())
                        .build();
            }
        };
        mapper.addConverter(myConverter);
    }
}
