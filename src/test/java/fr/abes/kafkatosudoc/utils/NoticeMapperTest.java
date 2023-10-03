package fr.abes.kafkatosudoc.utils;

import fr.abes.LigneKbartConnect;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.Exemplaire;
import fr.abes.cbs.notices.NoticeConcrete;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {NoticeMapper.class, UtilsMapper.class})
public class NoticeMapperTest {
    @Autowired
    UtilsMapper mapper;

    @Test
    @DisplayName("Test création notice from Kbart cas 1")
    void testMapperNoticeFromKbartCas1() {
        LigneKbartConnect kbart = new LigneKbartConnect();
        kbart.setONLINEIDENTIFIER("978-0-415-11262-8");
        kbart.setDATEMONOGRAPHPUBLISHEDONLIN("2023-01-01");
        kbart.setPUBLICATIONTITLE("Test title");
        kbart.setPUBLISHERNAME("Test publisher");
        kbart.setACCESSTYPE("F");
        kbart.setFIRSTAUTHOR("Test author");
        kbart.setFIRSTEDITOR("Test editor");
        kbart.setTITLEURL("https://www.test.com/10.1484/M.BM-EB.5.113206");

        NoticeConcrete notice = mapper.map(kbart, NoticeConcrete.class);
        Biblio biblio = notice.getNoticeBiblio();
        Assertions.assertEquals(18, biblio.getListeZones().size());
        //controle type de document
        Assertions.assertEquals("Oax3", biblio.findZones("008").get(0).findSubLabel("$a"));
        //controle ISBN
        Assertions.assertEquals("978-0-415-11262-8", biblio.findZones("010").get(0).findSubLabel("$A"));
        //controle DOI
        Assertions.assertEquals("10.1484/M.BM-EB.5.113206", biblio.findZones("017").get(0).findSubLabel("$a"));
        Assertions.assertEquals('7', biblio.findZones("017").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('0', biblio.findZones("017").get(0).getIndicateurs()[1]);

        //controle année de publication
        Assertions.assertEquals("2023", biblio.findZones("100").get(0).findSubLabel("$a"));
        Assertions.assertEquals('0', biblio.findZones("100").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("100").get(0).getIndicateurs()[1]);

        //controle langue de publication + pays
        Assertions.assertEquals("und", biblio.findZones("101").get(0).findSubLabel("$a"));
        Assertions.assertEquals("XX", biblio.findZones("102").get(0).findSubLabel("$a"));

        //controle données codées ressources électroniques
        Assertions.assertEquals("r", biblio.findZones("135").get(0).findSubLabel("$b"));
        Assertions.assertEquals("01", biblio.findZones("181").get(0).findSubLabel("$P"));
        Assertions.assertEquals("txt", biblio.findZones("181").get(0).findSubLabel("$c"));
        Assertions.assertEquals("01", biblio.findZones("182").get(0).findSubLabel("$P"));
        Assertions.assertEquals("c", biblio.findZones("182").get(0).findSubLabel("$c"));
        Assertions.assertEquals("01", biblio.findZones("183").get(0).findSubLabel("$P"));
        Assertions.assertEquals("ceb", biblio.findZones("183").get(0).findSubLabel("$a"));

        //controle titre
        Assertions.assertEquals("@Test title", biblio.findZones("200").get(0).findSubLabel("$a"));
        Assertions.assertEquals('1', biblio.findZones("200").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("200").get(0).getIndicateurs()[1]);

        //controle mention de publication
        Assertions.assertEquals("Test publisher", biblio.findZones("214").get(0).findSubLabel("$c"));
        Assertions.assertEquals("[Lieu de publication inconnu]", biblio.findZones("214").get(0).findSubLabel("$a"));
        Assertions.assertEquals('#', biblio.findZones("214").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('0', biblio.findZones("214").get(0).getIndicateurs()[1]);
        Assertions.assertEquals("[Lieu de diffusion inconnu]", biblio.findZones("214").get(1).findSubLabel("$a"));
        Assertions.assertEquals("2023", biblio.findZones("214").get(1).findSubLabel("$d"));
        Assertions.assertEquals('#', biblio.findZones("214").get(1).getIndicateurs()[0]);
        Assertions.assertEquals('2', biblio.findZones("214").get(1).getIndicateurs()[1]);

        //controle zone note auto générée
        Assertions.assertEquals("Notice générée automatiquement à partir des métadonnées de BACON. SUPPRIMER LA PRESENTE NOTE 309 APRES MISE A JOUR", biblio.findZones("309").get(0).findSubLabel("$a"));

        //controle note sur les conditions d'accès
        Assertions.assertEquals("Ressource en accès libre", biblio.findZones("371").get(0).findSubLabel("$a"));
        Assertions.assertEquals('0', biblio.findZones("371").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("371").get(0).getIndicateurs()[1]);

        //controle auteur
        Assertions.assertEquals("Test author", biblio.findZones("700").get(0).findSubLabel("$a"));
        Assertions.assertEquals("070", biblio.findZones("700").get(0).findSubLabel("$4"));
        Assertions.assertEquals('#', biblio.findZones("700").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('1', biblio.findZones("700").get(0).getIndicateurs()[1]);

        //controle editor
        Assertions.assertEquals("Test editor", biblio.findZones("701").get(0).findSubLabel("$a"));
        Assertions.assertEquals("651", biblio.findZones("701").get(0).findSubLabel("$4"));
        Assertions.assertEquals('#', biblio.findZones("701").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('1', biblio.findZones("701").get(0).getIndicateurs()[1]);

        //controle URL
        Assertions.assertEquals("https://www.test.com/10.1484/M.BM-EB.5.113206", biblio.findZones("856").get(0).findSubLabel("$u"));
        Assertions.assertEquals('4', biblio.findZones("856").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("856").get(0).getIndicateurs()[1]);

        Exemplaire exemplaire = notice.getExemplaires().get(0);
        Assertions.assertEquals(3, exemplaire.getListeZones().size());
        Assertions.assertEquals("x", exemplaire.findZones("e01").get(0).findSubLabel("$b"));
        Assertions.assertEquals("341725297", exemplaire.findZones("930").get(0).findSubLabel("$b"));
        Assertions.assertEquals("g", exemplaire.findZones("930").get(0).findSubLabel("$j"));
        Assertions.assertEquals("Exemplaire créé automatiquement par l'ABES", exemplaire.findZones("991").get(0).findSubLabel("$a"));

    }

    @Test
    @DisplayName("Test création notice from Kbart cas 2")
    void testMapperNoticeFromKbartCas2() {
        //type d'accès différent, et pas de DOI
        LigneKbartConnect kbart = new LigneKbartConnect();
        kbart.setONLINEIDENTIFIER("0-415-11262-8");
        kbart.setDATEMONOGRAPHPUBLISHEDONLIN("2023-01-01");
        kbart.setPUBLICATIONTITLE("Test title");
        kbart.setPUBLISHERNAME("Test publisher");
        kbart.setACCESSTYPE("P");
        kbart.setFIRSTAUTHOR("Test author");
        kbart.setFIRSTEDITOR("Test editor");
        kbart.setTITLEURL("https://www.test.com/");

        NoticeConcrete notice = mapper.map(kbart, NoticeConcrete.class);
        Biblio biblio = notice.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());

        //controle isbn
        Assertions.assertEquals("0-415-11262-8", biblio.findZones("010").get(0).findSubLabel("$a"));

        //controle note sur les conditions d'accès
        Assertions.assertEquals("Accès en ligne réservé aux établissements ou bibliothèques qui en ont fait l'acquisition", biblio.findZones("371").get(0).findSubLabel("$a"));
        Assertions.assertEquals('0', biblio.findZones("371").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("371").get(0).getIndicateurs()[1]);
    }

    @Test
    @DisplayName("Test création notice from Kbart cas 2 : un éditeur / pas d'auteur")
    void testMapperNoticeFromKbartCas3() {
        //type d'accès différent, et pas de DOI
        LigneKbartConnect kbart = new LigneKbartConnect();
        kbart.setONLINEIDENTIFIER("0-415-11262-8");
        kbart.setDATEMONOGRAPHPUBLISHEDONLIN("2023-01-01");
        kbart.setPUBLICATIONTITLE("Test title");
        kbart.setPUBLISHERNAME("Test publisher");
        kbart.setACCESSTYPE("P");
        kbart.setFIRSTAUTHOR("");
        kbart.setFIRSTEDITOR("Test éditeur");
        kbart.setTITLEURL("https://www.test.com/");

        NoticeConcrete notice = mapper.map(kbart, NoticeConcrete.class);
        Biblio biblio = notice.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());

        //controle isbn
        Assertions.assertEquals("Test éditeur", biblio.findZones("700").get(0).findSubLabel("$a"));
        Assertions.assertEquals("651", biblio.findZones("700").get(0).findSubLabel("$4"));

    }

}
