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

        Path report = service.createInsertionReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice)).orElseThrow();

        try (Workbook workbook = WorkbookFactory.create(
                report.toFile())) {
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
    void replacesPreviousRowsWithCurrentLoadAndPreservesHistoricalTxt() throws IOException {
        Path txt = tempDir.resolve("ErreursInsertion469.txt");
        Files.writeString(txt, "historique");
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

        service.createInsertionReport("FIRST_PACKAGE_2025-11-02.tsv",
                List.of(error469("111111111", "Erreur 1")),
                List.of(connectNotice("111111111", "Titre 1", "1111-1111", "2222-2222")));
        service.createInsertionReport("SECOND_PACKAGE_2025-11-03.tsv",
                List.of(error469("222222222", "Erreur 2")),
                List.of(connectNotice("222222222", "Titre 2", "3333-3333", "4444-4444")));

        List<Path> reports;
        try (Stream<Path> files = Files.walk(tempDir)) {
            reports = files.filter(path -> path.getFileName().toString()
                            .equals("ErreursInsertion469.xlsx"))
                    .toList();
        }
        assertEquals(2, reports.size());
        assertTrue(reports.stream().anyMatch(path ->
                "FIRST_PACKAGE_2025-11-02".equals(readBouquet(path))));
        assertTrue(reports.stream().anyMatch(path ->
                "SECOND_PACKAGE_2025-11-03".equals(readBouquet(path))));
        assertEquals("historique", Files.readString(txt));
    }

    @Test
    void createsCreationWorkbookFromConnectNotice() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.EXNIHILO, "{Ppn : 115575375, Erreur : Création impossible}");

        Path report = service.createCreationReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(connectNotice("115575375", "Titre ex nihilo", "1111-2222", "3333-4444")))
                .orElseThrow();

        assertCreationRow(report,
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

        Path report = service.createCreationReportFromPrint(
                "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice)).orElseThrow();

        assertCreationRow(report,
                List.of("262071878", "che ppn 262071878",
                        "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02", "Dérivation impossible",
                        "Titre imprimé", "5555-6666", "7777-8888"));
    }

    @Test
    void keepsUnavailableKbartFieldsEmptyAndDoesNotCreateForEmptyList() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        ErrorMessage globalError = new ErrorMessage(
                ERROR_TYPE.CONNEXION, "Erreur : Connexion CBS impossible");

        Path report = service.createInsertionReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(globalError),
                List.of()).orElseThrow();
        assertEquals(List.of(
                        "", "", "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
                        "Connexion CBS impossible", "", "", ""),
                workbookRow(report, 1));

        Path emptyDir = tempDir.resolve("empty");
        ErrorWorkbookService emptyService = new ErrorWorkbookService(emptyDir.toString());
        assertTrue(emptyService.createInsertionReport(
                "EMPTY.tsv", List.of(), List.of()).isEmpty());
        assertFalse(Files.exists(emptyDir));
    }

    @Test
    void leavesConnectKbartFieldsEmptyWhenErrorHasNoPpn() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        LigneKbartConnect notice = connectNotice(
                "", "Titre indû", "1111-2222", "3333-4444");
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.CONNEXION, "Erreur : Connexion CBS impossible");

        Path report = service.createInsertionReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice)).orElseThrow();

        assertEquals(List.of(
                        "", "", "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
                        "Connexion CBS impossible", "", "", ""),
                workbookRow(report, 1));
    }

    @Test
    void leavesPrintedKbartFieldsEmptyWhenErrorHasNoPpn() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        LigneKbartImprime notice = printedNotice(
                "", "Titre indu", "5555-6666", "7777-8888");
        ErrorMessage error = new ErrorMessage(
                ERROR_TYPE.FROMIMPRIME, "Erreur : Dérivation impossible");

        Path report = service.createCreationReportFromPrint(
                "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error),
                List.of(notice)).orElseThrow();

        assertEquals(List.of(
                        "", "", "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02",
                        "Dérivation impossible", "", "", ""),
                workbookRow(report, 1));
    }

    @Test
    void formatsWorkbookForSafeTextReading() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

        Path report = service.createInsertionReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error469("000000001", "Erreur formatée")),
                List.of(connectNotice("000000001", "Titre", "0012-3456", "0098-7654")))
                .orElseThrow();

        try (Workbook workbook = WorkbookFactory.create(
                report.toFile())) {
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
        assertFalse(Files.exists(report.resolveSibling("ErreursInsertion469.xlsx.tmp")));
    }

    @Test
    void configuresSameReadableColumnWidthsOnBothReports() throws IOException {
        ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
        int[] expectedWidths = {12, 20, 38, 50, 50, 16, 16};

        Path insertionReport = service.createInsertionReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(error469("000000001", "Erreur d'insertion")),
                List.of(connectNotice("000000001", "Titre", "0012-3456", "0098-7654")))
                .orElseThrow();
        Path creationReport = service.createCreationReport(
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
                List.of(new ErrorMessage(
                        ERROR_TYPE.EXNIHILO,
                        "{Ppn : 000000002, Erreur : Création impossible}")),
                List.of(connectNotice("000000002", "Titre", "0012-3456", "0098-7654")))
                .orElseThrow();

        for (Path workbookPath : List.of(insertionReport, creationReport)) {
            try (Workbook workbook = WorkbookFactory.create(workbookPath.toFile())) {
                Sheet sheet = workbook.getSheetAt(0);
                for (int column = 0; column < expectedWidths.length; column++) {
                    assertEquals(expectedWidths[column] * 256, sheet.getColumnWidth(column));
                }
            }
        }
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

    private String readBouquet(Path workbookPath) {
        try (Workbook workbook = WorkbookFactory.create(workbookPath.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            return sheet.getRow(1).getCell(2).getStringCellValue();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
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
