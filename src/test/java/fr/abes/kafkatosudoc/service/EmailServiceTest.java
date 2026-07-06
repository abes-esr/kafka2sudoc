package fr.abes.kafkatosudoc.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {

    private EmailService emailService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        emailService = new EmailService();
        // Injection de la valeur pathToErrors via reflection
        Field pathToErrorsField = EmailService.class.getDeclaredField("pathToErrors");
        pathToErrorsField.setAccessible(true);
        pathToErrorsField.set(emailService, tempDir.toString() + "/");
    }

    @Test
    @DisplayName("Test formatErrorLine avec un filename valide")
    void testFormatErrorLine() {
        String filename = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv";
        String ppn = "115575375";
        String erreur = "Les sous-zones $5/$a dans 606 s'excluent mutuellement";

        String result = emailService.formatErrorLine(filename, ppn, erreur);

        String expected = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02\tche ppn 115575375\t115575375\tLes sous-zones $5/$a dans 606 s'excluent mutuellement" + System.lineSeparator();
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test formatErrorLine avec PPN vide")
    void testFormatErrorLineWithEmptyPpn() {
        String filename = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv";
        String ppn = "";
        String erreur = "Erreur de connexion CBS";

        String result = emailService.formatErrorLine(filename, ppn, erreur);

        String expected = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02\t\t\tErreur de connexion CBS" + System.lineSeparator();
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Test formatErrorLine avec suffixe _FORCE")
    void testFormatErrorLineWithForceSuffix() {
        String filename = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02_FORCE.tsv";
        String ppn = "262071878";
        String erreur = "Mot attendu après '@' dans 200$a";

        String result = emailService.formatErrorLine(filename, ppn, erreur);

        // Le bouquet contient toujours _FORCE car on extrait provider/package/date depuis le filename
        assertNotNull(result);
        assertTrue(result.contains("che ppn 262071878"));
        assertTrue(result.contains("Mot attendu après '@' dans 200$a"));
    }

    @Test
    @DisplayName("Test extractPpn avec format ExNihilo {Ppn : xxx, Erreur : xxx}")
    void testExtractPpnExNihiloFormat() {
        String message = "{Ppn : 115575375, Erreur : Les sous-zones $5/$a dans 606 s'excluent mutuellement}";

        String ppn = emailService.extractPpn(message);

        assertEquals("115575375", ppn);
    }

    @Test
    @DisplayName("Test extractPpn avec format Imprime {Ppn : xxx, Erreur : xxx, Notice : xxx}")
    void testExtractPpnImprimeFormat() {
        String message = "{Ppn : 262071878, Erreur : Mot attendu après '@' dans 200$a, Notice : 008 $aOax3}";

        String ppn = emailService.extractPpn(message);

        assertEquals("262071878", ppn);
    }

    @Test
    @DisplayName("Test extractPpn avec format 469 {PPN:xxx,Erreur:xxx,...}")
    void testExtractPpn469Format() {
        String message = "{PPN:262071878,Erreur:Mot attendu après '@' dans 200$a,Ligne Kbart:...,Notice:...}";

        String ppn = emailService.extractPpn(message);

        assertEquals("262071878", ppn);
    }

    @Test
    @DisplayName("Test extractPpn avec message sans PPN")
    void testExtractPpnNoPpn() {
        String message = "Erreur : CBS connection failed";

        String ppn = emailService.extractPpn(message);

        assertEquals("", ppn);
    }

    @Test
    @DisplayName("Test extractErreur avec format ExNihilo {Ppn : xxx, Erreur : xxx}")
    void testExtractErreurExNihiloFormat() {
        String message = "{Ppn : 115575375, Erreur : Les sous-zones $5/$a dans 606 s'excluent mutuellement}";

        String erreur = emailService.extractErreur(message);

        assertEquals("Les sous-zones $5/$a dans 606 s'excluent mutuellement", erreur);
    }

    @Test
    @DisplayName("Test extractErreur avec format Imprime {Ppn : xxx, Erreur : xxx, Notice : xxx}")
    void testExtractErreurImprimeFormat() {
        String message = "{Ppn : 262071878, Erreur : Mot attendu après '@' dans 200$a, Notice : 008 $aOax3}";

        String erreur = emailService.extractErreur(message);

        assertEquals("Mot attendu après '@' dans 200$a", erreur);
    }

    @Test
    @DisplayName("Test extractErreur avec format 469 {PPN:xxx,Erreur:xxx,Ligne Kbart:xxx,Notice:xxx}")
    void testExtractErreur469Format() {
        String message = "{PPN:262071878,Erreur:Mot attendu après '@' dans 200$a,Ligne Kbart:...,Notice:...}";

        String erreur = emailService.extractErreur(message);

        assertEquals("Mot attendu après '@' dans 200$a", erreur);
    }

    @Test
    @DisplayName("Test extractErreur avec plain text (CONNEXION)")
    void testExtractErreurPlainText() {
        String message = "Erreur : CBS connection failed";

        String erreur = emailService.extractErreur(message);

        assertEquals("CBS connection failed", erreur);
    }

    @Test
    @DisplayName("Test appendErrorsToFile : création du fichier avec en-tête")
    void testAppendErrorsToFileCreation() throws IOException {
        String lines = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02\tche ppn 115575375\t115575375\tErreur test" + System.lineSeparator();

        emailService.appendErrorsToFile(lines, "ErreursInsertion469.txt");

        Path filePath = tempDir.resolve("ErreursInsertion469.txt");
        assertTrue(Files.exists(filePath));

        List<String> fileLines = Files.readAllLines(filePath);
        assertEquals(2, fileLines.size());
        assertEquals("Bouquet\tRequête WinIBW\tPPN\tErreur", fileLines.get(0));
        assertTrue(fileLines.get(1).contains("JSTOR_GLOBAL_ALLEBOOKS_2025-11-02"));
        assertTrue(fileLines.get(1).contains("che ppn 115575375"));
        assertTrue(fileLines.get(1).contains("Erreur test"));
    }

    @Test
    @DisplayName("Test appendErrorsToFile : ajout de lignes à un fichier existant")
    void testAppendErrorsToFileAppend() throws IOException {
        // Premier ajout (création)
        String lines1 = "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02\tche ppn 115575375\t115575375\tErreur 1" + System.lineSeparator();
        emailService.appendErrorsToFile(lines1, "ErreursCreations.txt");

        // Deuxième ajout (append)
        String lines2 = "CAIRN_GLOBAL_ALLEBOOKS_2025-11-03\tche ppn 241855942\t241855942\tErreur 2" + System.lineSeparator();
        emailService.appendErrorsToFile(lines2, "ErreursCreations.txt");

        Path filePath = tempDir.resolve("ErreursCreations.txt");
        assertTrue(Files.exists(filePath));

        List<String> fileLines = Files.readAllLines(filePath);
        // 1 en-tête + 2 lignes de données
        assertEquals(3, fileLines.size());
        assertEquals("Bouquet\tRequête WinIBW\tPPN\tErreur", fileLines.get(0));
        assertTrue(fileLines.get(1).contains("JSTOR"));
        assertTrue(fileLines.get(2).contains("CAIRN"));
    }

    @Test
    @DisplayName("Test appendErrorsToFile avec lignes vides (ne fait rien)")
    void testAppendErrorsToFileEmptyLines() throws IOException {
        emailService.appendErrorsToFile("", "ErreursInsertion469.txt");

        Path filePath = tempDir.resolve("ErreursInsertion469.txt");
        assertFalse(Files.exists(filePath));
    }
}
