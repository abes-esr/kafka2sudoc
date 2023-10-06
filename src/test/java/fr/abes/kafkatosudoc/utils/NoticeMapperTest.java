package fr.abes.kafkatosudoc.utils;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.Exemplaire;
import fr.abes.cbs.notices.NoticeConcrete;
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
        Assertions.assertEquals("https://www.test.com/10.1484/M.BM-EB.5.113206", biblio.findZones("859").get(0).findSubLabel("$u"));
        Assertions.assertEquals('4', biblio.findZones("859").get(0).getIndicateurs()[0]);
        Assertions.assertEquals('#', biblio.findZones("859").get(0).getIndicateurs()[1]);

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

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas minimal")
    void testMapperNoticeFromKbartAndImprimeMin() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(14, biblio.getListeZones().size());
        Assertions.assertEquals("Oax3", biblio.findZone("008", 0).findSubLabel("$a"));
        Assertions.assertEquals("0-415-11262-8", biblio.findZone("010", 0).findSubLabel("$a"));
        Assertions.assertEquals("2019", biblio.findZone("100", 0).findSubLabel("$a"));
        Assertions.assertEquals("fre", biblio.findZone("101", 0).findSubLabel("$a"));
        Assertions.assertEquals("XX", biblio.findZone("102", 0).findSubLabel("$a"));
        Assertions.assertEquals("r", biblio.findZone("135", 0).findSubLabel("$b"));
        Assertions.assertEquals("01", biblio.findZone("181", 0).findSubLabel("$P"));
        Assertions.assertEquals("txt", biblio.findZone("181", 0).findSubLabel("$c"));
        Assertions.assertEquals("01", biblio.findZone("182", 0).findSubLabel("$P"));
        Assertions.assertEquals("c", biblio.findZone("182", 0).findSubLabel("$c"));
        Assertions.assertEquals("01", biblio.findZone("183", 0).findSubLabel("$P"));
        Assertions.assertEquals("ceb", biblio.findZone("183", 0).findSubLabel("$a"));
        Assertions.assertEquals("Titre notice", biblio.findZone("200", 0).findSubLabel("$a"));
        Assertions.assertEquals("Lieu de diffusion inconnu", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("Accès en ligne réservé aux établissements ou bibliothèques qui en ont fait l'acquisition", biblio.findZone("371", 0).findSubLabel("$a"));
        Assertions.assertEquals("123456789", biblio.findZone("452", 0).findSubLabel("$0"));
        Assertions.assertEquals("http://www.test.com/", biblio.findZone("859", 0).findSubLabel("$u"));

    }


    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 1 : ajout doi ")
    void testMapperNoticeFromKbartAndImprimeCas1() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setTitleUrl(null);
        kbartAndImprimeDto.getKbart().setTitleId("10.1038/issn.1476-4687");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(14, biblio.getListeZones().size());
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
        Assertions.assertEquals(15, biblio.getListeZones().size());
        Assertions.assertEquals("test104$a", biblio.findZone("104", 0).findSubLabel("$a"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 3 : ajout zone 105 ")
    void testMapperNoticeFromKbartAndImprimeCas3() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("105", "$a", "test105$a", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
        Assertions.assertEquals("test105$a", biblio.findZone("105", 0).findSubLabel("$a"));

        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("105", "$b", "r");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
        Assertions.assertEquals("test105$a", biblio.findZone("105", 0).findSubLabel("$a"));
        kbartAndImprimeDto.getNotice().getNoticeBiblio().deleteSousZone("105", "$b");

        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("105", "$b", "m");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
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
        Assertions.assertEquals(15, biblio.getListeZones().size());
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
        Assertions.assertEquals(14, biblio.getListeZones().size());
        Assertions.assertEquals("Lieu de diffusion inconnu", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("publisher", biblio.findZone("214", 0).findSubLabel("$c"));

        //cas d'une 214 avec les bons indicateurs
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("214", "$a", "Lieu de publication test", new char[]{'#', '0'});
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$c", "Nom Editeur");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("214", "$d", "2019");
        noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(14, biblio.getListeZones().size());
        Assertions.assertEquals("Lieu de publication test", biblio.findZone("214", 0).findSubLabel("$a"));
        Assertions.assertEquals("Nom Editeur", biblio.findZone("214", 0).findSubLabel("$c"));
        Assertions.assertNull(biblio.findZone("214", 0).findSubLabel("$d"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 6 : gestion zone 328 ")
    void testMapperNoticeFromKbartAndImprimeCas6() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("328", "$a", "note de thèse");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addSousZone("328", "$z", "test");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
        Assertions.assertEquals("note de thèse", biblio.findZone("328", 0).findSubLabel("$a"));
        Assertions.assertEquals("Autre édition de", biblio.findZone("328", 0).findSubLabel("$z"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 7 : gestion zone 371 ")
    void testMapperNoticeFromKbartAndImprimeCas7() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setAccessType("F");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(14, biblio.getListeZones().size());
        Assertions.assertEquals("Ressource en accès libre", biblio.findZone("371", 0).findSubLabel("$a"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 8 : gestion zone 3XX ")
    void testMapperNoticeFromKbartAndImprimeCas8() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        //zones 3XX ne devant pas être ajoutées
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("301", "$a", "test301");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("303", "$a", "test303");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("305", "$a", "test305");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("306", "$a", "test306");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("307", "$a", "test307");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("308", "$a", "test308");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("309", "$a", "test309");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("310", "$a", "test310");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("311", "$a", "test311");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("312", "$a", "test312");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("315", "$a", "test315");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("316", "$a", "test316");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("317", "$a", "test317");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("320", "$a", "test320");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("321", "$a", "test321");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("322", "$a", "test322");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("323", "$a", "test323");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("324", "$a", "test324");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("325", "$a", "test325");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("336", "$a", "test336");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("345", "$a", "test345");
        //zones 3XX devant être ajoutées
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("302", "$a", "test302");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("304", "$a", "test304");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("313", "$a", "test313");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("314", "$a", "test314");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("327", "$a", "test327");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("328", "$a", "test328");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("330", "$a", "test330");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("332", "$a", "test332");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("334", "$a", "test334");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("338", "$b", "test338");
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("359", "$a", "test359");

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(25, biblio.getListeZones().size());
        Assertions.assertEquals("test302", biblio.findZone("302", 0).findSubLabel("$a"));
        Assertions.assertEquals("test304", biblio.findZone("304", 0).findSubLabel("$a"));
        Assertions.assertEquals("test313", biblio.findZone("313", 0).findSubLabel("$a"));
        Assertions.assertEquals("test314", biblio.findZone("314", 0).findSubLabel("$a"));
        Assertions.assertEquals("test327", biblio.findZone("327", 0).findSubLabel("$a"));
        Assertions.assertEquals("test328", biblio.findZone("328", 0).findSubLabel("$a"));
        Assertions.assertEquals("test330", biblio.findZone("330", 0).findSubLabel("$a"));
        Assertions.assertEquals("test332", biblio.findZone("332", 0).findSubLabel("$a"));
        Assertions.assertEquals("test334", biblio.findZone("334", 0).findSubLabel("$a"));
        Assertions.assertEquals("test338", biblio.findZone("338", 0).findSubLabel("$b"));
        Assertions.assertEquals("test359", biblio.findZone("359", 0).findSubLabel("$a"));
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
        Assertions.assertEquals(14, biblio.getListeZones().size());
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 10 : gestion zone 454 ")
    void testMapperNoticeFromKbartAndImprimeCas10() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getNotice().getNoticeBiblio().addZone("454", "$t", "titre traduit", new char[]{'#', '#'});

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(15, biblio.getListeZones().size());
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
        Assertions.assertEquals(15, biblio.getListeZones().size());
        Assertions.assertEquals("test500", biblio.findZone("500", 0).findSubLabel("$a"));
    }

    @Test
    @DisplayName("Test création notice from Kbart & notice imprimée cas 12 : gestion zone 859 ")
    void testMapperNoticeFromKbartAndImprimeCas12() throws ZoneException {
        KbartAndImprimeDto kbartAndImprimeDto = getKbartAndImprimeDto();
        kbartAndImprimeDto.getKbart().setTitleUrl(null);

        NoticeConcrete noticeResult = mapper.map(kbartAndImprimeDto, NoticeConcrete.class);
        Biblio biblio = noticeResult.getNoticeBiblio();
        Assertions.assertEquals(13, biblio.getListeZones().size());
        Assertions.assertNull(biblio.findZone("859", 0));
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
        noticeImprimee.addZone("101", "$a", "fre");
        noticeImprimee.addZone("200", "$a", "Titre notice");
        NoticeConcrete notice = new NoticeConcrete(noticeImprimee, null, Lists.newArrayList());

        KbartAndImprimeDto kbartAndImprimeDto = new KbartAndImprimeDto();
        kbartAndImprimeDto.setKbart(kbart);
        kbartAndImprimeDto.setNotice(notice);
        return kbartAndImprimeDto;
    }
}
