package fr.abes.kafkatosudoc.utils;



import fr.abes.LigneKbartConnect;
import fr.abes.LigneKbartImprime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String extractDoiFromConnect(LigneKbartConnect kbart) {
        return extractDOI(kbart.getTITLEURL(), kbart.getTITLEID());
    }

    public static String extractDoiFromImprime(LigneKbartImprime kbart) {
        return extractDOI(kbart.getTitleUrl(), kbart.getTitleId());
    }

    private static String extractDOI(CharSequence titleUrl, CharSequence titleId) {
        String doiPattern = "10.\\d{0,15}.\\d{0,15}.+";

        if (titleUrl != null && !titleUrl.isEmpty()){
            return findDoiWithPatternDoi(titleUrl, doiPattern);
        }
        if (titleId != null && !titleId.isEmpty()){
            return findDoiWithPatternDoi(titleId, doiPattern);
        }
        return "";
    }

    private static String findDoiWithPatternDoi(CharSequence titleUrl, String doiPattern) {
        Pattern pattern = Pattern.compile(doiPattern);
        Matcher matcher = pattern.matcher(titleUrl);
        if (matcher.find()) {
            return matcher.group(0);
        } else {
            return "";
        }
    }

    /**
     * Méthode permettant de supprimer les éventuels tirets présents dans un isbn
     * @param isbn
     * @return
     */
    public static String extractOnlineIdentifier(String isbn) {
        return isbn.replace("-", "");
    }

    public static String getYearFromDate(String date) {
        if (date == null || date.isEmpty())
            return "";
        return date.substring(0,4);

    }
}
