package fr.abes.kafkatosudoc.utils;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.cbs.notices.TYPE_NOTICE;
import fr.abes.cbs.notices.Zone;
import fr.abes.cbs.utilitaire.Constants;
import fr.abes.kafkatosudoc.dto.KbartAndImprimeDto;
import org.assertj.core.util.Lists;
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
        kbart.setONLINEIDENTIFIER("9780415112628");
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
        Assertions.assertEquals("978-0-4151-1262-8", biblio.findZones("010").get(0).findSubLabel("$A"));
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

        kbart.setACCESSTYPE("P");
        notice = mapper.map(kbart, NoticeConcrete.class);
        biblio = notice.getNoticeBiblio();
        //controle URL
        Assertions.assertEquals("https://www.test.com/10.1484/M.BM-EB.5.113206", biblio.findZones("859").get(0).findSubLabel("$u"));
        Assertions.assertEquals('4', biblio.findZones("859").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("859").get(0).getIndicateurs()[1]);

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
        Assertions.assertEquals("Accès en ligne réservé aux établissements ou bibliothèques ayant souscrit l'abonnement", biblio.findZones("371").get(0).findSubLabel("$a"));
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

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas minimal")
    void testMapperNoticeFromKbartAndImprimeMin() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("Oax3", biblio.findZone("008", 0).findSubLabel("$a"));
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));
        Assertions.assertEquals("20XX", biblio.findZone("100", 0).findSubLabel("$a"));
        Assertions.assertEquals("fre", biblio.findZone("101", 0).findSubLabel("$a"));
        Assertions.assertEquals("eng", biblio.findZone("101", 0).findSubLabel("$c"));
        Assertions.assertEquals("XX", biblio.findZone("102", 0).findSubLabel("$a"));
        Assertions.assertEquals("r", biblio.findZone("135", 0).findSubLabel("$b"));
        Assertions.assertEquals("01", biblio.findZone("181", 0).findSubLabel("$P"));
        Assertions.assertEquals("txt", biblio.findZone("181", 0).findSubLabel("$c"));
        Assertions.assertEquals("01", biblio.findZone("182", 0).findSubLabel("$P"));
        Assertions.assertEquals("c", biblio.findZone("182", 0).findSubLabel("$c"));
        Assertions.assertEquals("01", biblio.findZone("183", 0).findSubLabel("$P"));
        Assertions.assertEquals("ceb", biblio.findZone("183", 0).findSubLabel("$a"));
        Assertions.assertEquals("Titre notice", biblio.findZone("200", 0).findSubLabel("$a"));
        Assertions.assertEquals("[Lieu de publication inconnu]", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("Accès en ligne réservé aux établissements ou bibliothèques ayant souscrit l'abonnement", biblio.findZone("371", 0).findSubLabel("$a"));
        Assertions.assertEquals("123456789", biblio.findZone("452", 0).findSubLabel("$5"));
        Assertions.assertEquals("http://www.test.com/", biblio.findZone("859", 0).findSubLabel("$u"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 0 : ajout isbn ")
    void testMapperNoticeFromKbartAndImprimeCas0() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));

        kbartAndImprimeDto.getKbart().setOnlineIdentifier(null);
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée : gestion zones 500 ")
    void testMapperNoticeFromKbartAndImprimeZones500() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        Zone zone500 = new Zone("500", TYPE_NOTICE.BIBLIOGRAPHIQUE);
        zone500.addSubLabel("$3", "123456789");
        zone500.addSubLabel("$k", "testk");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone(zone500);

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));
        Assertions.assertEquals("123456789", biblio.findZone("500", 0).findSubLabel("$5"));
        Assertions.assertEquals("testk", biblio.findZone("500", 0).findSubLabel("$k"));
        Assertions.assertNull(biblio.findZone("500", 0).findSubLabel("$3"));

        kbartAndImprimeDto.getKbart().setOnlineIdentifier(null);
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée : gestion zones 600")
    void testMapperNoticeFromKbartAndImprimeZones600() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        Zone zone600 = new Zone("600", TYPE_NOTICE.BIBLIOGRAPHIQUE);
        zone600.addSubLabel("$3", "123456789");
        zone600.addSubLabel("$3", "987654321");
        zone600.addSubLabel("$2", "rameau");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone(zone600);

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));
        Assertions.assertEquals("600 $5123456789$5987654321$2rameau\r", biblio.findZone("600", 0).toString());
        Assertions.assertNull(biblio.findZone("600", 0).findSubLabel("$3"));

        kbartAndImprimeDto.getKbart().setOnlineIdentifier(null);
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 0 : ajout isbn ")
    void testMapperNoticeFromKbartAndImprimeZones454() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        Zone zone454 = new Zone("454", TYPE_NOTICE.BIBLIOGRAPHIQUE);
        zone454.addSubLabel("$t", "test");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone(zone454);

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));
        Assertions.assertEquals("test", biblio.findZone("454", 0).findSubLabel("$t"));

        kbartAndImprimeDto.getNotice().getNoticeBiblio().findZone("454", 0).addSubLabel("$0", "test");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));
        Assertions.assertNull(biblio.findZone("454", 0));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas sous zones translitérées")
    void testMapperNoticeFromKbartAndImprimeTranslit4() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals("701 $5123456789$607$7fa$44070 (Auteur)\r", biblio.findZone("701", 0).toString());
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 1 : ajout doi ")
    void testMapperNoticeFromKbartAndImprimeCas1() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setTitleUrl(null);
        kbartAndImprimeDto.getKbart().setTitleId("10.1038/issn.1476-4687");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("10.1038/issn.1476-4687", biblio.findZone("017", 0).findSubLabel("$a"));
        Assertions.assertEquals("DOI", biblio.findZone("017", 0).findSubLabel("$2"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 2 : ajout zone 104 ")
    void testMapperNoticeFromKbartAndImprimeCas2() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("104", "$a", "test104$a", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("test104$a", biblio.findZone("104", 0).findSubLabel("$a"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 3 : ajout zone 105 ")
    void testMapperNoticeFromKbartAndImprimeCas3() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("105", "$a", "test105$a", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("test105$a", biblio.findZone("105", 0).findSubLabel("$a"));

        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("105", "$b", "r");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("test105$a", biblio.findZone("105", 0).findSubLabel("$a"));
        kbartAndImprimeDto.getNotice().getNoticeBiblio().deleteSousZone("105", "$b");

        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("105", "$b", "m");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("test105$a", biblio.findZone("105", 0).findSubLabel("$a"));
        Assertions.assertEquals("v", biblio.findZone("105", 0).findSubLabel("$b"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 4 : ajout zone 205 ")
    void testMapperNoticeFromKbartAndImprimeCas4() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("205", "$a", "mention d'édition", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("205", "$b", "Autre mention d'édition");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("mention d'édition", biblio.findZone("205", 0).findSubLabel("$a"));
        Assertions.assertEquals("Autre mention d'édition", biblio.findZone("205", 0).findSubLabel("$b"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 5 : gestion zone 214 ")
    void testMapperNoticeFromKbartAndImprimeCas5() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setPublisherName("publisher");
        //cas d'une 214 dont les indicateurs ne correspondent pas à ce qui est attendu
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("214", "$a", "Lieu de publication", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("[Lieu de publication inconnu]", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("publisher", biblio.findZone("214", 0).findSubLabel("$c"));

        //cas d'une 214 avec les bons indicateurs #0
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("214", "$a", "Lieu de publication test", new char[]{'#', '0'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$c", "Nom Editeur");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$d", "2019");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("Lieu de publication test", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("Nom Editeur", biblio.findZone("214", 0).findSubLabel("$c"));
        Assertions.assertNull(biblio.findZone("214", 0).findSubLabel("$d"));

        kbartAndImprimeDto.getNotice().getNoticeBiblio().deleteZone("214");

        //cas d'une 214 avec les bons indicateurs #2
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("214", "$a", "Lieu de publication test 2", new char[]{'#', '2'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$c", "Nom Editeur 2");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$d", "2020");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("[Lieu de publication inconnu]", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("publisher", biblio.findZone("214", 0).findSubLabel("$c"));
        Assertions.assertEquals("[Lieu de diffusion inconnu]", biblio.findZone("214", 1).findSubLabel("$a"));
        Assertions.assertEquals("[20..]", biblio.findZone("214", 1).findSubLabel("$d"));

        kbartAndImprimeDto.getNotice().getNoticeBiblio().deleteZone("214");

        //cas d'une 214 #0 avec plusieurs $a / $c
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("214", "$a", "Lieu de publication test", new char[]{'#', '0'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$c", "Nom Editeur");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$d", "2019");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$a", "Lieu de publication test 2");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$c", "Nom Editeur 2");

        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();

        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("Lieu de publication test", biblio.findZone("214", 0).getSubLabelList().get("$a").get(0));
        Assertions.assertEquals("Nom Editeur", biblio.findZone("214", 0).getSubLabelList().get("$c").get(0));

        Assertions.assertEquals("Lieu de publication test 2", biblio.findZone("214", 0).getSubLabelList().get("$a").get(1));
        Assertions.assertEquals("Nom Editeur 2", biblio.findZone("214", 0).getSubLabelList().get("$c").get(1));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 6 : gestion zone 328 ")
    void testMapperNoticeFromKbartAndImprimeCas6() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("328", "$a", "note de thèse");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("328", "$z", "test");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("note de thèse", biblio.findZone("328", 0).findSubLabel("$a"));
        Assertions.assertEquals("Autre édition de", biblio.findZone("328", 0).findSubLabel("$z"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 7 : gestion zone 371 + 856")
    void testMapperNoticeFromKbartAndImprimeCas7() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setAccessType("F");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
        Assertions.assertEquals("Ressource en accès libre", biblio.findZone("371", 0).findSubLabel("$a"));
        Assertions.assertEquals("http://www.test.com/", biblio.findZone("856", 0).findSubLabel("$u"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 9 : gestion zone 4XX ")
    void testMapperNoticeFromKbartAndImprimeCas9() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setAccessType("F");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("410", "$a", "test410");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("437", "$a", "test437");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(16, biblio.getListeZones().size());
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 10 : gestion zone 454 ")
    void testMapperNoticeFromKbartAndImprimeCas10() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("454", "$t", "titre traduit", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("titre traduit", biblio.findZone("454", 0).findSubLabel("$t"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 11 : gestion zone 5XX ")
    void testMapperNoticeFromKbartAndImprimeCas11() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("500", "$a", "test500", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("579", "$1", "test579", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("512", "$a", "test512", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("516", "$a", "test516", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("test500", biblio.findZone("500", 0).findSubLabel("$a"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 12 : gestion zone 859 ")
    void testMapperNoticeFromKbartAndImprimeCas12() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setTitleUrl(null);

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
        Assertions.assertNull(biblio.findZone("859", 0));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 13 : gestion zone 7XX ")
    void testMapperNoticeFromKbartAndImprimeCas13() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("700", "$a", "test700", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("710", "$a", "test710", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("702", "$a", "test702", new char[]{'#', '#'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("716", "$a", "test716", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(18, biblio.getListeZones().size());
        Assertions.assertEquals("test700", biblio.findZone("700", 0).findSubLabel("$a"));
        Assertions.assertEquals("test710", biblio.findZone("710", 0).findSubLabel("$a"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 14 : gestion zone 210 ")
    void testMapperNoticeFromKbartAndImprimeCas14() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("210", "$a", "Diffuseur");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().findZone("210", 0).addSubLabel("$c", "provider");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("Diffuseur", biblio.findZone("214", 2).findSubLabel("$a"));
        Assertions.assertEquals("provider", biblio.findZone("214", 2).findSubLabel("$c"));
        Assertions.assertEquals("#0", String.valueOf(biblio.findZone("214", 2).getIndicateurs()));

        kbartAndImprimeDto.getNotice().getNoticeBiblio().findZone("210", 0).deleteSubLabel("$c");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(17, biblio.getListeZones().size());
        Assertions.assertEquals("Diffuseur", biblio.findZone("214", 2).findSubLabel("$a"));
        Assertions.assertNull(biblio.findZone("214", 2).findSubLabel("$c"));
        Assertions.assertEquals("#0", String.valueOf(biblio.findZone("214", 2).getIndicateurs()));
    }

    private static KbartAndImprimeDto getKbartAndImprimeDto() throws ZoneException {
        LigneKbartImprime kbart = new LigneKbartImprime();
        kbart.setOnlineIdentifier("0-415-11262-8");
        kbart.setPublicationTitle("Test title");
        kbart.setAccessType("P");
        kbart.setTitleUrl("http://www.test.com/");
        kbart.setPpn("123456789");

        Biblio noticeImprimee = new Biblio();
        noticeImprimee.addZone("100", "$a", "2019");
        Zone zone101 = new Zone("101", TYPE_NOTICE.BIBLIOGRAPHIQUE);
        zone101.addSubLabel("$a", "fre");
        zone101.addSubLabel("$a", "ger");
        zone101.addSubLabel("$c", "eng");
        noticeImprimee.addZone(zone101);
        noticeImprimee.addZone("200", "$a", "Titre notice");
        Zone zone701 = new Zone("701", TYPE_NOTICE.BIBLIOGRAPHIQUE);
        zone701.addSubLabel("$3", "123456789");
        zone701.addSubLabel("$6", "07");
        zone701.addSubLabel("$7", "fa");
        zone701.addSubLabel("$4", "4070 (Auteur)");
        noticeImprimee.addZone(zone701);
        NoticeConcrete notice = new NoticeConcrete(noticeImprimee, null, Lists.newArrayList());

        KbartAndImprimeDto kbartAndImprimeDto = new KbartAndImprimeDto();
        kbartAndImprimeDto.setKbart(kbart);
        kbartAndImprimeDto.setNotice(notice);
        return kbartAndImprimeDto;
    }

    @Test
    void testMapperErreurZone101() throws ZoneException {
        String biblioStr = "000 $022,54\r" +
                "003 201657368\r" +
                "004 601592101:07-06-17\r" +
                "005 1999:08-01-20 18:00:12.000\r" +
                "006 601592101:07-06-17\r" +
                "008 $aAax3\r" +
                "00A $00\r" +
                "00U $0utf8\r" +
                "010 ##$A978-1-68108-131-1$brel.\r" +
                "010 ##$a1-68108-131-8$brel.\r" +
                "034 $aOCoLC$0990181308\r" +
                "035 ##$aocn967683336\r" +
                "100 0#$a2015$f2015\r" +
                "101 0#$aeng\r" +
                "102 ##$aAE\r" +
                "104 ##$ak$by$cy$dba$e0$ffre\r" +
                "105 ##$aa$ba$c0$d0$e1$fy$gy\r" +
                "106 ##$ar\r" +
                "181 ##$P01$ctxt\r" +
                "182 ##$P01$cn\r" +
                "183 ##$P01$anga\r" +
                "200 1#$a@Supercooling, crystallization and melting within emulsions and divided systems$emass, heat transfers and stability$fauthored by Danièle Clausse,... & Jean Pierre Dumas,...$g[foreword Johan Sjöblom]\r" +
                "214 #0$aCharjah$cBentham Science Publishers\r" +
                "214 #4$dC 2015\r" +
                "215 ##$a1 vol. (VII-290 p.)$cill. en noir et en coul., graph., fig., couv. ill. en coul.$d25 cm\r" +
                "320 ##$aBibliogr. en fin de chapitres. Index\r" +
                "606 ##$3027673235"+ Constants.STR_1B + "I@" +"testest$2rameau\r" +
                "606 ##$3031067980$2rameau\r" +
                "606 ##$302783168X$2rameau\r" +
                "606 ##$3033153604$2rameau\r" +
                "606 ##$3027831728$2rameau\r" +
                "606 ##$3027831817$2rameau\r" +
                "606 ##$3027372332$2rameau\r" +
                "676 ##$a660.294514$v23\r" +
                "680 ##$aTP156.E6\r" +
                "700 #1$3032988230$4070\r" +
                "701 #1$3059883006$4070\r" +
                "702 #1$3057101019$4080\r" +
                "801 #0$bYDX$gAACR2\r" +
                "801 #1$bYDX$gAACR2\r" +
                "801 #2$bOCLCQ$gAACR2\r";
        Biblio biblio = new Biblio(biblioStr);
        NoticeConcrete notice = new NoticeConcrete(biblio, null, Lists.newArrayList());

        LigneKbartImprime kbart = new LigneKbartImprime();
        kbart.setOnlineIdentifier("0-415-11262-8");
        kbart.setPublicationTitle("Test title");
        kbart.setAccessType("P");
        kbart.setTitleUrl("http://www.test.com/");
        kbart.setPpn("123456789");

        KbartAndImprimeDto kbartAndImprimeDto = new KbartAndImprimeDto();
        kbartAndImprimeDto.setKbart(kbart);
        kbartAndImprimeDto.setNotice(notice);

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Assertions.assertEquals("101 0#$aeng\r", noticeResult.getNoticeBiblio().findZone("101", 0).toString());
        Assertions.assertEquals("606 ##$5027673235$2rameau\r", noticeResult.getNoticeBiblio().findZone("606", 0).toString());


    }
}
