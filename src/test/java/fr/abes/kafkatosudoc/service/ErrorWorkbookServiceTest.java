package fr.abes.kafkatosudoc.service;

import fr.abes.LigneKbartConnect;
import fr.abes.kafkatosudoc.dto.ERROR_TYPE;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private LigneKbartConnect connectNotice(
            String ppn, String title, String printIssn, String onlineIssn) {
        LigneKbartConnect notice = new LigneKbartConnect();
        notice.setBESTPPN(ppn);
        notice.setPUBLICATIONTITLE(title);
        notice.setPRINTIDENTIFIER(printIssn);
        notice.setONLINEIDENTIFIER(onlineIssn);
        return notice;
    }

    private List<String> rowValues(Row row) {
        List<String> values = new ArrayList<>();
        row.cellIterator().forEachRemaining(cell -> values.add(cell.getStringCellValue()));
        return values;
    }
}
