package fr.abes.kafkatosudoc.service;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorWorkbookServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createsInsertionWorkbookWithKbartData() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        LigneKbartConnect notice = connectNotice(
                "262071878", "Titre électronique", "1234-5678", "8765-4321");
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.ADD469,
                "{PPN:262071878,Erreur:Zone 469 invalide,Ligne Kbart:...,Notice:...}");

        assertTrue(service.appendInsertionErrors(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice)));

        try (Workbook workbook = WorkbookFactory.create(
                service.insertionWorkbookPath().toFile())) {
            Sheet sheet = workbook.getSheet("ErreursInsertion469");
            assertEquals(List.of(
                    "PPN", "Commande WinIBW", "Bouquet", "Erreur",
                    "Titre", "ISSN imprimé", "ISSN en ligne"),
                    rowValues(sheet.getRow(0)));
            assertEquals(List.of(
                    "262071878",
                    "che ppn 262071878",
                    "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
                    "Zone 469 invalide",
                    "Titre électronique",
                    "1234-5678",
                    "8765-4321"),
                    rowValues(sheet.getRow(1)));
        }
    }

    @Test
    void appendsRowsAndPreservesHistoricalTxt() throws IOException {
        Path txt = tempDir.resolve("ErreursInsertion469.txt");
        Files.writeString(txt, "historique");
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

        service.appendInsertionErrors("FIRST_PACKAGE_2025-11-02.tsv",
                List.of(error469("111111111", "Erreur 1")),
                List.of(connectNotice("111111111", "Titre 1", "1111-1111", "2222-2222")));
        service.appendInsertionErrors("SECOND_PACKAGE_2025-11-03.tsv",
                List.of(error469("222222222", "Erreur 2")),
                List.of(connectNotice("222222222", "Titre 2", "3333-3333", "4444-4444")));

        try (Workbook workbook = WorkbookFactory.create(
                service.insertionWorkbookPath().toFile())) {
            assertEquals(2, workbook.getSheetAt(0).getLastRowNum());
        }
        assertEquals("historique", Files.readString(txt));
    }

    @Test
    void createsCreationWorkbookFromConnectNotice() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.EXNIHILO, "{Ppn : 115575375, Erreur : Création impossible}");

        assertTrue(service.appendCreationErrors(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(connectNotice("115575375", "Titre ex nihilo", "1111-2222", "3333-4444"))));

        assertCreationRow(service.creationWorkbookPath(),
                List.of("115575375", "che ppn 115575375",
                        "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02", "Création impossible",
                        "Titre ex nihilo", "1111-2222", "3333-4444"));
    }

    @Test
    void createsCreationWorkbookFromPrintedNotice() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        LigneKbartImprime notice = printedNotice(
                "262071878", "Titre imprimé", "5555-6666", "7777-8888");
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.FROMIMPRIME,
                "{Ppn : 262071878, Erreur : Dérivation impossible}");

        assertTrue(service.appendCreationErrorsFromPrint(
                "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice)));

        assertCreationRow(service.creationWorkbookPath(),
                List.of("262071878", "che ppn 262071878",
                        "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02", "Dérivation impossible",
                        "Titre imprimé", "5555-6666", "7777-8888"));
    }

    @Test
    void keepsUnavailableKbartFieldsEmptyAndDoesNotCreateForEmptyList() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        ErrorMessage globalError = new ErrorMessage(
                ERROR_TYPE.CONNEXION, "Erreur : Connexion CBS impossible");

        assertTrue(service.appendInsertionErrors(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(globalError),
                List.of()));
        assertEquals(List.of(
                        "", "", "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
                        "Connexion CBS impossible", "", "", ""),
                workbookRow(service.insertionWorkbookPath(), 1));

        Path emptyDir = tempDir.resolve("empty");
        ErrorWorkbookService emptyService = new ErrorWorkbookService(emptyDir.toString());
        assertFalse(emptyService.appendInsertionErrors("EMPTY.tsv", List.of(), List.of()));
        assertFalse(Files.exists(emptyService.insertionWorkbookPath()));
    }

    @Test
    void leavesConnectKbartFieldsEmptyWhenErrorHasNoPpn() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        LigneKbartConnect notice = connectNotice(
                "", "Titre indû", "1111-2222", "3333-4444");
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.CONNEXION, "Erreur : Connexion CBS impossible");

        service.appendInsertionErrors(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice));

        assertEquals(List.of(
                        "", "", "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
                        "Connexion CBS impossible", "", "", ""),
                workbookRow(service.insertionWorkbookPath(), 1));
    }

    @Test
    void leavesPrintedKbartFieldsEmptyWhenErrorHasNoPpn() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        LigneKbartImprime notice = printedNotice(
                "", "Titre indu", "5555-6666", "7777-8888");
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.FROMIMPRIME, "Erreur : Dérivation impossible");

        service.appendCreationErrorsFromPrint(
                "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice));

        assertEquals(List.of(
                        "", "", "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02",
                        "Dérivation impossible", "", "", ""),
                workbookRow(service.creationWorkbookPath(), 1));
    }

    @Test
    void formatsWorkbookForSafeTextReading() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

        service.appendInsertionErrors(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error469("000000001", "Erreur formatée")),
                List.of(connectNotice("000000001", "Titre", "0012-3456", "0098-7654")));

        try (Workbook workbook = WorkbookFactory.create(
                service.insertionWorkbookPath().toFile())) {
            XSSFSheet sheet = (XSSFSheet) workbook.getSheet("ErreursInsertion469");
            Row header = sheet.getRow(0);
            for (Cell cell : header) {
                assertEquals(IndexedColors.BLUE.getIndex(),
                        cell.getCellStyle().getFillForegroundColor());
                assertEquals(FillPatternType.SOLID_FOREGROUND,
                        cell.getCellStyle().getFillPattern());
                assertEquals(IndexedColors.WHITE.getIndex(),
                        workbook.getFontAt(cell.getCellStyle().getFontIndex()).getColor());
            }
            for (Cell cell : sheet.getRow(1)) {
                assertEquals(CellType.STRING, cell.getCellType());
                assertEquals("@", cell.getCellStyle().getDataFormatString());
            }
            assertNotNull(sheet.getPaneInformation());
            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertEquals(1, sheet.getPaneInformation().getHorizontalSplitPosition());
            assertTrue(sheet.getCTWorksheet().isSetAutoFilter());
            assertEquals("A1:G2", sheet.getCTWorksheet().getAutoFilter().getRef());
        }
        assertFalse(Files.exists(tempDir.resolve("ErreursInsertion469.xlsx.tmp")));
    }

    @ParameterizedTest
    @MethodSource("supportedErrorFormats")
    void extractsPpnAndErrorFromEverySupportedFormat(
            String message, String expectedPpn, String expectedError) {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

        assertEquals(expectedPpn, service.extractPpn(message));
        assertEquals(expectedError, service.extractError(message));
    }

    private static Stream<Arguments> supportedErrorFormats() {
        return Stream.of(
                Arguments.of(
                        "{Ppn : 115575375, Erreur : Création impossible}",
                        "115575375", "Création impossible"),
                Arguments.of(
                        "{Ppn : 262071878, Erreur : Dérivation impossible, Notice : 008 $aOax3}",
                        "262071878", "Dérivation impossible"),
                Arguments.of(
                        "{PPN:262071878,Erreur:Zone 469 invalide,Ligne Kbart:...,Notice:...}",
                        "262071878", "Zone 469 invalide"),
                Arguments.of(
                        "Erreur : CBS connection failed",
                        "", "CBS connection failed"));
    }

    private ErrorMessage error469(String ppn, String error) {
        return new ErrorMessage(
                ERROR_TYPE.ADD469,
                "{PPN:" + ppn + ",Erreur:" + error + ",Ligne Kbart:...,Notice:...}");
    }

    private LigneKbartConnect connectNotice(
            String ppn, String title, String printIssn, String onlineIssn) {
        LigneKbartConnect notice = new LigneKbartConnect();
        notice.setBESTPPN(ppn);
        notice.setPUBLICATIONTITLE(title);
        notice.setPRINTIDENTIFIER(printIssn);
        notice.setONLINEIDENTIFIER(onlineIssn);
        return notice;
    }

    private LigneKbartImprime printedNotice(
            String ppn, String title, String printIssn, String onlineIssn) {
        LigneKbartImprime notice = new LigneKbartImprime();
        notice.setPpn(ppn);
        notice.setPublicationTitle(title);
        notice.setPrintIdentifier(printIssn);
        notice.setOnlineIdentifier(onlineIssn);
        return notice;
    }

    private void assertCreationRow(Path workbookPath, List<String> expected) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(workbookPath.toFile())) {
            assertEquals(expected, rowValues(workbook.getSheet("ErreursCreations").getRow(1)));
        }
    }

    private List<String> workbookRow(Path workbookPath, int rowIndex) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(workbookPath.toFile())) {
            return rowValues(workbook.getSheetAt(0).getRow(rowIndex));
        }
    }

    private List<String> rowValues(Row row) {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            values.add(row.getCell(index).getStringCellValue());
        }
        return values;
    }
}
