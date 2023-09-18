package fr.abes.kafkatosudoc.utils;

import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.Exemplaire;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.kafkatosudoc.dto.LigneKbartDto;
import lombok.SneakyThrows;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

public class NoticeMapper {
    private final UtilsMapper mapper;

    public NoticeMapper(UtilsMapper mapper) { this.mapper = mapper; }

    @Bean
    public void converterKbartToNoticeConcrete() {
        Converter<LigneKbartDto, NoticeConcrete> myConverter = new Converter<LigneKbartDto, NoticeConcrete>() {
            @SneakyThrows
            public NoticeConcrete convert(MappingContext<LigneKbartDto, NoticeConcrete> context) {
                LigneKbartDto kbart = context.getSource();
                //création de la notice biblio
                Biblio noticeBiblio = new Biblio();
                //ajout zone type de document
                noticeBiblio.addZone("008", "$a", "Oax3");
                //ajout ISBN
                if ((kbart.getOnlineIdentifier().length() == 10)) {
                    noticeBiblio.addZone("010", "$a", kbart.getOnlineIdentifier());
                } else {
                    noticeBiblio.addZone("010", "$A", kbart.getOnlineIdentifier());
                }

                //DOI
                String doi =  Utils.extractDOI(kbart);
                if (!doi.equals("")) {
                    noticeBiblio.addZone("017", "$a", doi, new char[]{'7', '0'});
                    noticeBiblio.addSousZone("017", "$2", "DOI");
                }
                //Date de publication
                if (kbart.getDateMonographPublishedOnline() != null)
                    noticeBiblio.addZone("100", "$a", kbart.getDateMonographPublishedOnline(), new char[]{'0', '#'});

                //Langue de publication
                noticeBiblio.addZone("101", "$a", "und");
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

                noticeBiblio.addZone("200", "$a", "@" + kbart.getPublicationTitle(), new char[]{'1', '#'});
                //Mention de publication / diffusion
                if (kbart.getPublisherName() != null)
                    noticeBiblio.addZone("214", "$c", kbart.getPublisherName(), new char[]{'#', '0'});
                else
                    noticeBiblio.addZone("214", "$c", "[Lieu de publication inconnu");

                noticeBiblio.addZone("214", "$a", "[Lieu de diffusion inconnu]", new char[]{'#', '2'});
                noticeBiblio.addSousZone("214", "$d", kbart.getDateMonographPublishedOnline(), 1);
                noticeBiblio.addZone("309", "$a", "Notice générée automatiquement à partir des métadonnées de BACON. SUPPRIMER LA PRESENTE NOTE 309 APRES MISE A JOUR");

                //Note sur les conditions d'accès
                if (kbart.getAccessType().equals("F"))
                    noticeBiblio.addZone("371", "$a", "Ressource en accès libre", new char[]{'0', '#'});
                else
                    noticeBiblio.addZone("371", "$a", "Accès en ligne réservé aux établissements ou bibliothèques qui en ont fait l'acquisition", new char[]{'0', '#'});

                //1er auteur
                noticeBiblio.addZone("700", "$a", kbart.getFirstAuthor(), new char[]{'1', '#'});
                noticeBiblio.addSousZone("700", "$4", "070");

                //editeur
                noticeBiblio.addZone("701", "$a", kbart.getFirstEditor(), new char[]{'1', '#'});
                noticeBiblio.addSousZone("701", "$6", "51");

                //url d'accès
                noticeBiblio.addZone("856", "$u", kbart.getTitleUrl(), new char[]{'4', '#'});

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
