package fr.abes.kafkatosudoc.service;

import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;
import fr.abes.kafkatosudoc.dto.ErrorMessage;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static final List<Integer> COLUMN_WIDTHS = List.of(
            12, 20, 38, 50, 50, 16, 16);
    private static final Pattern CREATION_PPN_PATTERN = Pattern.compile("Ppn\\s*:\\s*([^,}]+)");
    private static final Pattern INSERTION_PPN_PATTERN = Pattern.compile("PPN:([^,}]+)");
    private static final Pattern CREATION_ERROR_PATTERN =
            Pattern.compile("Erreur\\s*:\\s*(.+?)(?:, Notice|\\})");
    private static final Pattern INSERTION_ERROR_PATTERN =
            Pattern.compile("Erreur:(.+),Ligne Kbart:");
    private static final Pattern BRACED_ERROR_PATTERN = Pattern.compile("Erreur:(.+)\\}");
    private static final Pattern PLAIN_ERROR_PATTERN = Pattern.compile("Erreur\\s*:\\s*(.+)");

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

    public synchronized boolean appendCreationErrors(
            String filename,
            List<ErrorMessage> errors,
            List<LigneKbartConnect> notices) throws IOException {
        return append(
                errors.stream().map(error -> connectRow(filename, error, notices)).toList(),
                creationWorkbookPath(),
                "ErreursCreations");
    }

    public synchronized boolean appendCreationErrorsFromPrint(
            String filename,
            List<ErrorMessage> errors,
            List<LigneKbartImprime> notices) throws IOException {
        return append(
                errors.stream().map(error -> printedRow(filename, error, notices)).toList(),
                creationWorkbookPath(),
                "ErreursCreations");
    }

    public Path insertionWorkbookPath() {
        return outputDirectory.resolve(INSERTION_FILENAME);
    }

    public Path creationWorkbookPath() {
        return outputDirectory.resolve(CREATION_FILENAME);
    }

    private ErrorWorkbookRow connectRow(
            String filename,
            ErrorMessage error,
            List<LigneKbartConnect> notices) {
        String ppn = extractPpn(error.getMessage());
        String errorText = extractError(error.getMessage());
        LigneKbartConnect notice = ppn.isEmpty()
                ? null
                : notices.stream()
                        .filter(candidate -> candidate.getBESTPPN() != null)
                        .filter(candidate -> ppn.contentEquals(candidate.getBESTPPN()))
                        .findFirst()
                        .orElse(null);

        return new ErrorWorkbookRow(
                ppn,
                command(ppn),
                bouquet(filename),
                errorText,
                value(notice == null ? null : notice.getPUBLICATIONTITLE()),
                value(notice == null ? null : notice.getPRINTIDENTIFIER()),
                value(notice == null ? null : notice.getONLINEIDENTIFIER()));
    }

    private ErrorWorkbookRow printedRow(
            String filename,
            ErrorMessage error,
            List<LigneKbartImprime> notices) {
        String ppn = extractPpn(error.getMessage());
        LigneKbartImprime notice = ppn.isEmpty()
                ? null
                : notices.stream()
                        .filter(candidate -> candidate.getPpn() != null)
                        .filter(candidate -> ppn.contentEquals(candidate.getPpn()))
                        .findFirst()
                        .orElse(null);

        return new ErrorWorkbookRow(
                ppn,
                command(ppn),
                bouquet(filename),
                extractError(error.getMessage()),
                value(notice == null ? null : notice.getPublicationTitle()),
                value(notice == null ? null : notice.getPrintIdentifier()),
                value(notice == null ? null : notice.getOnlineIdentifier()));
    }

    private boolean append(
            List<ErrorWorkbookRow> rows,
            Path workbookPath,
            String sheetName) throws IOException {
        if (rows.isEmpty()) {
            return false;
        }
        Files.createDirectories(workbookPath.getParent());
        Path temporaryPath = workbookPath.resolveSibling(
                workbookPath.getFileName() + ".tmp");
        try (Workbook workbook = Files.exists(workbookPath)
                ? WorkbookFactory.create(workbookPath.toFile())
                : new XSSFWorkbook()) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = createSheet(workbook, sheetName);
            }
            configureColumnWidths(sheet);
            CellStyle textStyle = textStyle(workbook);
            int rowIndex = sheet.getLastRowNum() + 1;
            for (ErrorWorkbookRow row : rows) {
                writeRow(sheet.createRow(rowIndex++), row.values(), textStyle);
            }
            updateAutoFilter(sheet);
            try (OutputStream output = Files.newOutputStream(temporaryPath)) {
                workbook.write(output);
            }
        } catch (IOException exception) {
            Files.deleteIfExists(temporaryPath);
            throw exception;
        }
        replaceAtomically(temporaryPath, workbookPath);
        return true;
    }

    private Sheet createSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeRow(sheet.createRow(0), HEADERS, headerStyle(workbook));
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private void writeRow(Row row, List<String> values, CellStyle style) {
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
            row.getCell(index).setCellStyle(style);
        }
    }

    private void configureColumnWidths(Sheet sheet) {
        for (int column = 0; column < COLUMN_WIDTHS.size(); column++) {
            sheet.setColumnWidth(column, COLUMN_WIDTHS.get(column) * 256);
        }
    }

    private CellStyle textStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("@"));
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void updateAutoFilter(Sheet sheet) {
        sheet.setAutoFilter(new CellRangeAddress(
                0,
                sheet.getLastRowNum(),
                0,
                HEADERS.size() - 1));
    }

    private void replaceAtomically(Path temporaryPath, Path workbookPath) throws IOException {
        try {
            Files.move(
                    temporaryPath,
                    workbookPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporaryPath,
                    workbookPath,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String bouquet(String filename) {
        return String.join("_",
                CheckFiles.getProviderFromFilename(filename),
                CheckFiles.getPackageFromFilename(filename),
                CheckFiles.extractDateString(filename));
    }

    String extractPpn(String message) {
        String creationPpn = extract(CREATION_PPN_PATTERN, message);
        return creationPpn.isEmpty()
                ? extract(INSERTION_PPN_PATTERN, message)
                : creationPpn;
    }

    String extractError(String message) {
        for (Pattern pattern : List.of(
                INSERTION_ERROR_PATTERN,
                CREATION_ERROR_PATTERN,
                BRACED_ERROR_PATTERN,
                PLAIN_ERROR_PATTERN)) {
            String error = extract(pattern, message);
            if (!error.isEmpty()) {
                return error;
            }
        }
        return message;
    }

    private String extract(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(value(message));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String command(String ppn) {
        return ppn.isEmpty() ? "" : "che ppn " + ppn;
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
