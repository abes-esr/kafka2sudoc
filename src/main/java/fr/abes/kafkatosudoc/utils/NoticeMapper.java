package fr.abes.kafkatosudoc.utils;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.*;
import fr.abes.kafkatosudoc.dto.KbartAndImprimeDto;
import lombok.SneakyThrows;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class NoticeMapper {
    private final UtilsMapper mapper;

    public NoticeMapper(UtilsMapper mapper) { this.mapper = mapper; }

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
                if ((Utils.extractOnlineIdentifier(kbart.getONLINEIDENTIFIER().toString()).length() == 10)) {
                    noticeBiblio.addZone("010", "$a", kbart.getONLINEIDENTIFIER().toString());
                } else {
                    noticeBiblio.addZone("010", "$A", kbart.getONLINEIDENTIFIER().toString());
                }

                //DOI
                String doi =  Utils.extractDoiFromConnect(kbart);
                if (!doi.equals("")) {
                    noticeBiblio.addZone("017", "$a", doi, new char[]{'7', '0'});
                    noticeBiblio.addSousZone("017", "$2", "DOI");
                }
                //Date de publication
                if (kbart.getDATEMONOGRAPHPUBLISHEDONLIN() != null)
                    noticeBiblio.addZone("100", "$a", Utils.getYearFromDate(kbart.getDATEMONOGRAPHPUBLISHEDONLIN().toString()), new char[]{'0', '#'});

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
                //Mention de publication / diffusion
                noticeBiblio.addZone("214", "$a", "[Lieu de publication inconnu]", new char[]{'#', '0'});
                if (kbart.getPUBLISHERNAME() != null)
                    noticeBiblio.addSousZone("214", "$c", kbart.getPUBLISHERNAME().toString());


                noticeBiblio.addZone("214", "$a", "[Lieu de diffusion inconnu]", new char[]{'#', '2'});
                noticeBiblio.addSousZone("214", "$d", Utils.getYearFromDate(kbart.getDATEMONOGRAPHPUBLISHEDONLIN().toString()), 1);
                noticeBiblio.addZone("309", "$a", "Notice générée automatiquement à partir des métadonnées de BACON. SUPPRIMER LA PRESENTE NOTE 309 APRES MISE A JOUR");

                //Note sur les conditions d'accès
                if (kbart.getACCESSTYPE().equals("F"))
                    noticeBiblio.addZone("371", "$a", "Ressource en accès libre", new char[]{'0', '#'});
                else
                    noticeBiblio.addZone("371", "$a", "Accès en ligne réservé aux établissements ou bibliothèques qui en ont fait l'acquisition", new char[]{'0', '#'});

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
                noticeBiblio.addZone("859", "$u", kbart.getTITLEURL().toString(), new char[]{'4', '#'});
                NoticeConcrete notice = new NoticeConcrete(noticeBiblio, null, generateExemplaire());
                return notice;
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
                if ((Utils.extractOnlineIdentifier(kbart.getOnlineIdentifier().toString()).length() == 10)) {
                    noticeElec.addZone("010", "$a", kbart.getOnlineIdentifier().toString());
                } else {
                    noticeElec.addZone("010", "$A", kbart.getOnlineIdentifier().toString());
                }

                //DOI
                String doi =  Utils.extractDoiFromImprime(kbart);
                if (!doi.equals("")) {
                    noticeElec.addZone("017", "$a", doi, new char[]{'7', '0'});
                    noticeElec.addSousZone("017", "$2", "DOI");
                }

                //ajout date de publication
                noticeElec.addZone(noticeImprimee.getNoticeBiblio().findZone("100", 0));
                //langue de publication
                noticeElec.addZone("101", "$a", noticeImprimee.getNoticeBiblio().findZone("101", 0).findSubLabel("$a"), new char[]{'0', '#'});
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
                List<Zone> listZone214 = noticeImprimee.getNoticeBiblio().findZones("214").stream().filter(zone -> Arrays.toString(zone.getIndicateurs()).toString().equals("[#, 0]")).toList();
                if (listZone214.isEmpty()) {
                    noticeElec.addZone("214", "$a", "Lieu de diffusion inconnu", new char[]{'#', '0'});
                    if (kbart.getPublisherName() != null)
                        noticeElec.addSousZone("214", "$c", kbart.getPublisherName().toString());
                }
                for (Zone zone214 : listZone214) {
                    Zone zone214Elec = new Zone("214", TYPE_NOTICE.BIBLIOGRAPHIQUE);
                    String lieuPublication = zone214.findSubLabel("$a");
                    String nomEditeur = zone214.findSubLabel("$c");
                    if (lieuPublication != null) {
                        zone214Elec.addSubLabel("$a", lieuPublication);
                        if (nomEditeur != null)
                            zone214Elec.addSubLabel("$c", nomEditeur);
                        else {
                            if (kbart.getPublisherName() != null)
                                zone214Elec.addSubLabel("$c", kbart.getPublisherName().toString());
                        }
                    }
                    else {
                        zone214Elec.addSubLabel("$a", "[Lieu de publication inconnu]");
                        if (nomEditeur != null)
                            zone214Elec.addSubLabel("$c", nomEditeur);
                        else {
                            if (kbart.getPublisherName() != null)
                                zone214Elec.addSubLabel("$c", kbart.getPublisherName().toString());
                        }
                    }
                    noticeElec.addZone(zone214Elec);
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
                if (kbart.getAccessType().equals("F"))
                    noticeElec.addZone("371", "$a", "Ressource en accès libre", new char[]{'0', '#'});
                else
                    noticeElec.addZone("371", "$a", "Accès en ligne réservé aux établissements ou bibliothèques qui en ont fait l'acquisition", new char[]{'0', '#'});

                noticeImprimee.getNoticeBiblio().findZones("300").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("302").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("304").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("313").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("314").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("327").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("330").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("332").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("334").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("338").forEach(noticeElec::addZone);
                noticeImprimee.getNoticeBiblio().findZones("359").forEach(noticeElec::addZone);

                noticeElec.addZone("452", "$0", kbart.getPpn().toString(), new char[]{'#', '#'});

                Zone zone454 = noticeImprimee.getNoticeBiblio().findZone("454", 0);
                if (zone454 != null) {
                    String zone454dollart = zone454.findSubLabel("$t");
                    if (zone454dollart != null)
                        noticeElec.addZone("454", "$t", zone454dollart, new char[]{'#', '#'});
                }
                //zone 5XX sauf 579, 512 et 516
                List<Zone> zones500 = noticeImprimee.getNoticeBiblio().getListeZones().values().stream().filter(zone -> zone.getLabel().startsWith("5")).filter(zone -> (!zone.getLabel().equals("579") && !zone.getLabel().equals("512") && !zone.getLabel().equals("516"))).toList();
                zones500.forEach(noticeElec::addZone);

                List<Zone> zones600 = noticeImprimee.getNoticeBiblio().getListeZones().values().stream().filter(zone -> zone.getLabel().startsWith("6")).toList();
                zones600.forEach(noticeElec::addZone);

                List<Zone> zones700 = noticeImprimee.getNoticeBiblio().getListeZones().values().stream().filter(zone -> zone.getLabel().startsWith("7")).toList();
                zones700.forEach(noticeElec::addZone);

                //url d'accès
                if (kbart.getTitleUrl() != null)
                    noticeElec.addZone("859", "$u", kbart.getTitleUrl().toString(), new char[]{'4', '#'});

                NoticeConcrete notice = new NoticeConcrete(noticeElec, null, generateExemplaire());
                return notice;
            }
        };
        mapper.addConverter(myConverter);
    }

    private List<Exemplaire> generateExemplaire() throws ZoneException {
        Exemplaire exemp = new Exemplaire();
        exemp.addZone("e01", "$b", "x");
        exemp.addZone("930", "$b", "341725297");
        exemp.addSousZone("930", "$j", "g");
        exemp.addZone("991", "$a", "Exemplaire créé automatiquement par l'ABES");
        List<Exemplaire> exemplaires = new ArrayList<>();
        exemplaires.add(exemp);
        return exemplaires;
    }
}
