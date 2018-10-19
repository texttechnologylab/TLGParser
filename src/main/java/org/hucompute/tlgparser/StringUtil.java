package org.hucompute.tlgparser;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class StringUtil {

    public static String encodeXml(String pString) {
        return pString.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String encodeBF(String pString) {
        return pString.replace("[", "(").replace("]", ")").replace("¤", " ").replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }

    public static String decodeBF(String pString) {
        return pString.replace("╔", "[").replace("╗", "]").replace("╦","\t");
    }

    public static String zonedDateTime2String(ZonedDateTime pZonedDateTime) {
        return pZonedDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static ZonedDateTime string2ZonedDateTime(String pString) {
        return ZonedDateTime.parse(pString, DateTimeFormatter.ISO_DATE_TIME);
    }

    public static long zonedDateTime2Long(ZonedDateTime pZonedDateTime) {
        return pZonedDateTime.toInstant().toEpochMilli();
    }

    public static String clearControlCharacters(String pString) {
        StringBuilder lResult = new StringBuilder();
        for (char c:pString.toCharArray()) {
            if (!Character.isISOControl(c)) {
                lResult.append(c);
            }
        }
        return lResult.toString();
    }

    public static Set<String> loadStringSet(File pFile) throws IOException {
        Set<String> lResult = new HashSet<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pFile), Charset.forName("UTF-8")));
        String lLine = null;
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.trim();
            if (lLine.length() > 0) {
                lResult.add(lLine);
            }
        }
        lReader.close();
        return lResult;
    }

    public static String getLongestCommonSubstring(String pA, String pB) {
        int lStart = 0;
        int lMax = 0;
        for (int i=0; i<pA.length(); i++) {
            for (int j=0; j<pB.length(); j++) {
                int x = 0;
                while (pA.charAt(i+x) == pB.charAt(j+x)) {
                    x++;
                    if (((i+x) >= pA.length()) || ((j+x) >= pB.length())) break;
                }
                if (x > lMax) {
                    lMax = x;
                    lStart = i;
                }
            }
        }
        return pA.substring(lStart, (lStart+lMax));
    }

    public static int getLongestCommonSubstringLength(String pA, String pB) {
        int lStart = 0;
        int lMax = 0;
        for (int i=0; i<pA.length(); i++) {
            for (int j=0; j<pB.length(); j++) {
                int x = 0;
                while (pA.charAt(i+x) == pB.charAt(j+x)) {
                    x++;
                    if (((i+x) >= pA.length()) || ((j+x) >= pB.length())) break;
                }
                if (x > lMax) {
                    lMax = x;
                    lStart = i;
                }
            }
        }
        return lMax;
    }

    public static double getCosineSimilarity(TObjectIntHashMap<String> pTokensA, TObjectIntHashMap<String> pTokensB) {
        Set<String> lTokens = new HashSet<>(pTokensA.keySet());
        lTokens.addAll(pTokensB.keySet());
        double lZaehler = 0;
        double lASquared = 0;
        double lBSquared = 0;
        for (String lToken:lTokens) {
            int lFreqA = pTokensA.containsKey(lToken) ? pTokensA.get(lToken) : 0;
            int lFreqB = pTokensB.containsKey(lToken) ? pTokensB.get(lToken) : 0;
            lZaehler += lFreqA*lFreqB;
            lASquared += lFreqA*lFreqA;
            lBSquared += lFreqB*lFreqB;
        }
        double lNenner = Math.sqrt(lASquared) * Math.sqrt(lBSquared);
        return lNenner <= 0 ? 0 : lZaehler/lNenner;
    }

    public static boolean isNumber(String pString) {
        try {
            Double.parseDouble(pString);
            return true;
        }
        catch (NumberFormatException e) {
            try {
                Long.parseLong(pString);
                return true;
            }
            catch (NumberFormatException f) {
                return false;
            }
        }
    }

    public static String getString(File pFile) throws IOException {
        StringBuilder lResult = new StringBuilder();
        char[] lBuffer = new char[65536];
        int lRead = 0;
        InputStreamReader lReader = new InputStreamReader(new FileInputStream(pFile), Charset.forName("UTF-8"));
        while ((lRead = lReader.read(lBuffer)) > 0) {
            lResult.append(lBuffer, 0, lRead);
        }
        return lResult.toString();
    }

}
