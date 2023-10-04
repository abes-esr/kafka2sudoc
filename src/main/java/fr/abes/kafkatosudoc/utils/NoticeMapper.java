package fr.abes.kafkatosudoc.utils;

import fr.abes.LigneKbartConnect;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.Exemplaire;
import fr.abes.cbs.notices.NoticeConcrete;
import lombok.SneakyThrows;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
                String doi =  Utils.extractDOI(kbart);
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
                if (kbart.getPUBLICATIONTITLE() != null)
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
                noticeBiblio.addZone("856", "$u", kbart.getTITLEURL().toString(), new char[]{'4', '#'});

                //création de l'exemplaire pour affichage dans le Sudoc public
                Exemplaire exemp = new Exemplaire();
                exemp.addZone("e01", "$b", "x");
                exemp.addZone("930", "$b", "341725297");
                exemp.addSousZone("930", "$j", "g");
                exemp.addZone("991", "$a", "Exemplaire créé automatiquement par l'ABES");
                List<Exemplaire> exemplaires = new ArrayList<>();
                exemplaires.add(exemp);
                NoticeConcrete notice = new NoticeConcrete(noticeBiblio, null, exemplaires);
                return notice;
            }
        };
        mapper.addConverter(myConverter);
    }
}
