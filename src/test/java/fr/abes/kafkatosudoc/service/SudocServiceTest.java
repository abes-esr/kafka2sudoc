package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.process.ProcessCBS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {SudocService.class})
class SudocServiceTest {
    @MockBean
    ProcessCBS cbs;

    @Autowired
    SudocService sudocService;

    @Test
    void getNoticeBouquetException() throws CBSException {
        Mockito.when(cbs.search(Mockito.anyString())).thenThrow(new CBSException("V/VERROR", "Erreur Recherche"));
        Assertions.assertThrows(CBSException.class, () -> sudocService.getNoticeBouquet("CAIRN", "GLOBAL"));
    }

    @Test
    void getNoticeBouquetUneNotice() throws CBSException {
        Mockito.when(cbs.search(Mockito.anyString())).thenReturn("test");
        Mockito.when(cbs.getNbNotices()).thenReturn(1);
        Mockito.when(cbs.getPpnEncours()).thenReturn("111111111");
        Assertions.assertEquals("111111111", sudocService.getNoticeBouquet("CAIRN", "GLOBAL"));
    }

    @Test
    void getNoticeBouquetPlusieursNotices() throws CBSException {
        Mockito.when(cbs.search(Mockito.anyString())).thenReturn("test");
        Mockito.when(cbs.getNbNotices()).thenReturn(2);
        CBSException result = assertThrows(CBSException.class, () -> sudocService.getNoticeBouquet("CAIRN", "GLOBAL"));
        Assertions.assertEquals("V/VERROR", result.getCodeErreur());
        Assertions.assertEquals("Provider : CAIRN / package : GLOBAL : Recherche de la notice bouquet échouée", result.getMessage());
    }

    @Test
    void isNoticeBouquetInBestPpnTestTrue() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "123456789");
        Assertions.assertTrue(sudocService.isNoticeBouquetInBestPpn(notice, "123456789"));
    }

    @Test
    void isNoticeBouquetInBestPpnTestFalse() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "987654321");
        Assertions.assertFalse(sudocService.isNoticeBouquetInBestPpn(notice, "123456789"));
    }

    @Test
    void supprimerNoticeBouquetInBestPpnTest1() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "987654321");
        Biblio noticeResult = sudocService.supprimeNoticeBouquetInBestPpn(notice, "987654321");
        Assertions.assertEquals(0, noticeResult.getListeZones().size());
    }

    @Test
    void supprimerNoticeBouquetInBestPpnTest2() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "123456789");
        Biblio noticeResult = sudocService.supprimeNoticeBouquetInBestPpn(notice, "987654321");
        Assertions.assertEquals(1, noticeResult.getListeZones().size());
        Assertions.assertEquals("469", noticeResult.findZones("469").get(0).getLabel());
    }
}