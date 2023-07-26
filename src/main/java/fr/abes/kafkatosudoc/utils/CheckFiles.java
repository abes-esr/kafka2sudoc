package fr.abes.kafkatosudoc.utils;

import java.util.Arrays;
import java.util.List;

public class CheckFiles {
    public static String getProviderFromFilename(String filename) {
        String[] filenameFields = filename.split("_");
        if (filenameFields.length > 0)
            return filenameFields[0];
        return "";
    }

    public static String getPackageFromFilename(String filename) {
        String[] filenameFields = filename.substring(0, filename.lastIndexOf(".")).split("_");
        if (filenameFields.length > 1) {
            String [] filenameFieldsWithoutProvider = Arrays.copyOfRange(filenameFields, 1, filenameFields.length);
            return Arrays.stream(filenameFieldsWithoutProvider).takeWhile(field -> !field.matches("\\d{4}-\\d{2}-\\d{2}")).reduce("", (partialString, element) -> partialString + "_" + element).substring(1);
        }
        return "";
    }
    public static String getDateFromFile(String dateToComplement, String filename) {
        if (dateToComplement.length() == 4) {
            List<String> dateInFileName = List.of(filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf(".tsv")).split("-"));

            return dateInFileName.get(2) + "/" + dateInFileName.get(1) + "/" + dateToComplement;
        }
        return dateToComplement;
    }
}
