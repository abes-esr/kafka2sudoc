package fr.abes.kafkatosudoc.utils;

import fr.abes.kafkatosudoc.dto.LigneKbartDto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String extractDOI(LigneKbartDto kbart) {
        String doiPattern = "10.\\d{0,15}.\\d{0,15}.+";

        if (kbart.getTitleUrl() != null && !kbart.getTitleUrl().isEmpty()){
            Pattern pattern = Pattern.compile(doiPattern);
            Matcher matcher = pattern.matcher(kbart.getTitleUrl());
            if (matcher.find()) {
                return matcher.group(0);
            } else {
                return "";
            }
        }
        if (kbart.getTitleId() != null && !kbart.getTitleId().isEmpty()){
            Pattern pattern = Pattern.compile(doiPattern);
            Matcher matcher = pattern.matcher(kbart.getTitleId());
            if (matcher.find()) {
                return matcher.group(0);
            } else {
                return "";
            }
        }
        return "";
    }
}
