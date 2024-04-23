package fr.abes.kafkatosudoc.service;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.process.ProcessCBS;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SudocServiceTest {
    ProcessCBS cbs;

    SudocService sudocService;

    @BeforeEach
    void init() {
        this.cbs = Mockito.mock(ProcessCBS.class);
        this.sudocService = new SudocService(this.cbs);

    }
    @Test
    void getNoticeBouquetException() throws IOException {
        Mockito.when(cbs.search(Mockito.anyString())).thenThrow(new IOException("Erreur Recherche"));
        Assertions.assertThrows(IOException.class, () -> sudocService.getNoticeBouquet("CAIRN", "GLOBAL"));
    }

    @Test
    void getNoticeBouquetUneNotice() throws CBSException, IOException {
        Mockito.when(cbs.search(Mockito.anyString())).thenReturn("test");
        Mockito.when(cbs.getNbNotices()).thenReturn(1);
        Mockito.when(cbs.getPpnEncours()).thenReturn("111111111");
        Assertions.assertEquals("111111111", sudocService.getNoticeBouquet("CAIRN", "GLOBAL"));
    }

    @Test
    void getNoticeBouquetPlusieursNotices() throws IOException {
        Mockito.when(cbs.search(Mockito.anyString())).thenReturn("test");
        Mockito.when(cbs.getNbNotices()).thenReturn(2);
        CBSException result = assertThrows(CBSException.class, () -> sudocService.getNoticeBouquet("CAIRN", "GLOBAL"));
        Assertions.assertEquals(Level.WARN, result.getCodeErreur());
        Assertions.assertEquals("Provider : CAIRN / package : GLOBAL : Recherche de la notice bouquet échouée", result.getMessage());
    }

    @Test
    void isNoticeBouquetInBestPpnTestTrue() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "123456789");
        Assertions.assertTrue(sudocService.isNoticeBouquetInPpn(notice, "123456789"));
    }

    @Test
    void isNoticeBouquetInBestPpnTestFalse() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "987654321");
        Assertions.assertFalse(sudocService.isNoticeBouquetInPpn(notice, "123456789"));
    }

    @Test
    void supprimerNoticeBouquetInBestPpnTest1() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "987654321");
        Biblio noticeResult = sudocService.supprimeNoticeBouquetInPpn(notice, "987654321");
        Assertions.assertEquals(0, noticeResult.getListeZones().size());
    }

    @Test
    void supprimerNoticeBouquetInBestPpnTest2() throws ZoneException {
        Biblio notice = new Biblio();
        notice.addZone("469", "0", "123456789");
        Biblio noticeResult = sudocService.supprimeNoticeBouquetInPpn(notice, "987654321");
        Assertions.assertEquals(1, noticeResult.getListeZones().size());
        Assertions.assertEquals("469", noticeResult.findZones("469").get(0).getLabel());
    }
}