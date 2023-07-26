package fr.abes.kafkatosudoc.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CheckFilesTest {

    @Test
    void getPackageFromFilename() {
        String filename = "CYBERLIBRIS_cde53_mono_resu_2023-06-13.tsv";
        Assertions.assertEquals("cde53_mono_resu", CheckFiles.getPackageFromFilename(filename));

        filename = "CYBERLIBRIS_cde53_mono_resu_2023-06-13_FORCE.tsv";
        Assertions.assertEquals("cde53_mono_resu", CheckFiles.getPackageFromFilename(filename));
    }
}