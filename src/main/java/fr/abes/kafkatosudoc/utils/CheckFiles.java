package fr.abes.kafkatosudoc.utils;

import fr.abes.kafkatosudoc.exception.IllegalDateException;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static Date extractDate(String filename) throws IllegalDateException {
        Date date = new Date();
        try {
            Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(filename);
            if(matcher.find()){
                date = new SimpleDateFormat("yyyy-MM-dd").parse(matcher.group(1));
            }
            return date;
        } catch (Exception e) {
            throw new IllegalDateException(e);
        }
    }

    public static String extractDateString(String filename) {
        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE).matcher(filename);
        if(matcher.find()){
            return matcher.group(1);
        }
        return "";
    }
}
