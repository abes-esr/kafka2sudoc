package fr.abes.kafkatosudoc.utils;

import fr.abes.kafkatosudoc.dto.connect.LigneKbartConnect;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String extractDOI(LigneKbartConnect kbart) {
        String doiPattern = "10.\\d{0,15}.\\d{0,15}.+";

        if (kbart.getTITLEURL() != null && !kbart.getTITLEURL().isEmpty()){
            Pattern pattern = Pattern.compile(doiPattern);
            Matcher matcher = pattern.matcher(kbart.getTITLEURL());
            if (matcher.find()) {
                return matcher.group(0);
            } else {
                return "";
            }
        }
        if (kbart.getTITLEID() != null && !kbart.getTITLEID().isEmpty()){
            Pattern pattern = Pattern.compile(doiPattern);
            Matcher matcher = pattern.matcher(kbart.getTITLEID());
            if (matcher.find()) {
                return matcher.group(0);
            } else {
                return "";
            }
        }
        return "";
    }

    /**
     * Méthode permettant de supprimer les éventuels tirets présents dans un isbn
     * @param isbn
     * @return
     */
    public static String extractOnlineIdentifier(String isbn) {
        return isbn.replace("-", "");
    }
}
