package fr.abes.kafkatosudoc.service;

import fr.abes.LigneKbartConnect;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ErrorWorkbookService {
    static final String INSERTION_FILENAME = "ErreursInsertion469.xlsx";
    static final String CREATION_FILENAME = "ErreursCreations.xlsx";
    private static final List<String> HEADERS = List.of(
            "PPN", "Commande WinIBW", "Bouquet", "Erreur",
            "Titre", "ISSN imprimé", "ISSN en ligne");
    private static final Pattern PPN_PATTERN = Pattern.compile("PPN:([^,}]+)");
    private static final Pattern ERROR_PATTERN = Pattern.compile("Erreur:(.+),Ligne Kbart:");

    private final Path outputDirectory;

    public ErrorWorkbookService(
            @Value("${abes.pathToErrors:tempLog/}") String pathToErrors) {
        this.outputDirectory = Path.of(pathToErrors);
    }

    public synchronized boolean appendInsertionErrors(
            String filename,
            List<ErrorMessage> errors,
            List<LigneKbartConnect> notices) throws IOException {
        List<ErrorWorkbookRow> rows = errors.stream()
                .map(error -> connectRow(filename, error, notices))
                .toList();
        return append(rows, insertionWorkbookPath(), "ErreursInsertion469");
    }

    public Path insertionWorkbookPath() {
        return outputDirectory.resolve(INSERTION_FILENAME);
    }

    private ErrorWorkbookRow connectRow(
            String filename,
            ErrorMessage error,
            List<LigneKbartConnect> notices) {
        String ppn = extract(PPN_PATTERN, error.getMessage());
        String errorText = extract(ERROR_PATTERN, error.getMessage());
        LigneKbartConnect notice = notices.stream()
                .filter(candidate -> candidate.getBESTPPN() != null)
                .filter(candidate -> ppn.contentEquals(candidate.getBESTPPN()))
                .findFirst()
                .orElse(null);

        return new ErrorWorkbookRow(
                ppn,
                "che ppn " + ppn,
                bouquet(filename),
                errorText,
                value(notice == null ? null : notice.getPUBLICATIONTITLE()),
                value(notice == null ? null : notice.getPRINTIDENTIFIER()),
                value(notice == null ? null : notice.getONLINEIDENTIFIER()));
    }

    private boolean append(
            List<ErrorWorkbookRow> rows,
            Path workbookPath,
            String sheetName) throws IOException {
        Files.createDirectories(outputDirectory);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeRow(sheet.createRow(0), HEADERS);
            for (int index = 0; index < rows.size(); index++) {
                writeRow(sheet.createRow(index + 1), rows.get(index).values());
            }
            try (OutputStream output = Files.newOutputStream(workbookPath)) {
                workbook.write(output);
            }
        }
        return true;
    }

    private void writeRow(Row row, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    private String bouquet(String filename) {
        return String.join("_",
                CheckFiles.getProviderFromFilename(filename),
                CheckFiles.getPackageFromFilename(filename),
                CheckFiles.extractDateString(filename));
    }

    private String extract(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String value(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private record ErrorWorkbookRow(
            String ppn,
            String command,
            String bouquet,
            String error,
            String title,
            String printIssn,
            String onlineIssn) {
        private List<String> values() {
            return List.of(ppn, command, bouquet, error, title, printIssn, onlineIssn);
        }
    }
}
