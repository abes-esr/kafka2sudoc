package fr.abes.kafkatosudoc.utils;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilsTest {

    @Test
    void extractDOItestAvecPresenceDOIdanstitleUrl() {
        LigneKbartConnect kbart = new LigneKbartConnect();

        kbart.setTITLEURL("https://doi.org/10.1006/jmbi.1998.2354");

        Assertions.assertEquals("10.1006/jmbi.1998.2354", Utils.extractDoiFromConnect(kbart));

        kbart.setTITLEURL(null);
        Assertions.assertEquals("", Utils.extractDoiFromConnect(kbart));
    }

    @Test
    void extractDOItestAvecPresenceDOIdanstitleId() {
        LigneKbartConnect kbart = new LigneKbartConnect();
        kbart.setTITLEID("https://doi.org/10.1006/jmbi.1998.2354");

        Assertions.assertEquals("10.1006/jmbi.1998.2354", Utils.extractDoiFromConnect(kbart));

        kbart.setTITLEID(null);
        Assertions.assertEquals("", Utils.extractDoiFromConnect(kbart));
    }

    @Test
    void extractDOItestAvecPresenceDOIdanstitleUrlMaisSansPrefixeDOI() {
        LigneKbartConnect kbart = new LigneKbartConnect();
        kbart.setPUBLICATIONTITLE("testtitle");
        kbart.setPUBLICATIONTYPE("testtype");
        kbart.setONLINEIDENTIFIER("10.1006/jmbi.1998.2354");
        kbart.setPRINTIDENTIFIER("print");

        kbart.setTITLEURL("10.1006/jmbi.1998.2354");

        Assertions.assertEquals("10.1006/jmbi.1998.2354", Utils.extractDoiFromConnect(kbart));
    }

    @Test
    void extractDOItestAvecPresenceDOIdanstitleIdetTitleurl_priorisationTitleUrl() {
        LigneKbartConnect kbart = new LigneKbartConnect();
        kbart.setPUBLICATIONTITLE("testtitle");
        kbart.setPUBLICATIONTYPE("testtype");
        kbart.setONLINEIDENTIFIER("online");
        kbart.setPRINTIDENTIFIER("print");

        kbart.setTITLEID("https://doi.org/10.51257/a-v2-r7420");
        kbart.setTITLEURL("https://doi.org/10.1038/issn.1476-4687");

        Assertions.assertEquals("10.1038/issn.1476-4687", Utils.extractDoiFromConnect(kbart));
    }
    @Test
    void extractDOIImprime() {
        LigneKbartImprime kbart = new LigneKbartImprime();
        kbart.setPublicationTitle("testtitle");
        kbart.setPublicationType("testtype");
        kbart.setOnlineIdentifier("online");
        kbart.setPrintIdentifier("print");

        kbart.setTitleId("https://doi.org/10.51257/a-v2-r7420");
        kbart.setTitleUrl("https://doi.org/10.1038/issn.1476-4687");

        Assertions.assertEquals("10.1038/issn.1476-4687", Utils.extractDoiFromImprime(kbart));
    }
    @Test
    void getYearFromDateTest() {
        Assertions.assertEquals("2019", Utils.getYearFromDate("2019-01-01"));
        Assertions.assertEquals("", Utils.getYearFromDate(null));
        Assertions.assertEquals("", Utils.getYearFromDate(""));
    }
}
