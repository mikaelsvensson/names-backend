package info.mikaelsvensson.babyname.service.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class NameFeature {
    private static final String SWEDISH_EXTRA_VOWELS = "ÅÄÖåäö";
    private static final String SWEDISH_VOWELS = "AEIOUYaeiouy" + SWEDISH_EXTRA_VOWELS;
    private static final Pattern SWEDISH_VOWEL_GROUP = Pattern.compile("[" + SWEDISH_VOWELS + "]+");

    public static int syllableCount(String name) {
        var i = 0;
        final var matcher = SWEDISH_VOWEL_GROUP.matcher(normalize(name));
        while (matcher.find()) {
            i++;
        }
        return i;
    }

    private static final Pattern ACCENT_CHARS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String normalize(String name) {

        // Escape Swedish vowels so that they don't get normalize to A's and O's.
        for (char c : SWEDISH_EXTRA_VOWELS.toCharArray()) {
            name = name.replace(Character.toString(c), "[" + (int) c + "]");
        }

        name = ACCENT_CHARS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");

        // De-escape Swedish vowels back to normal.
        for (char c : SWEDISH_EXTRA_VOWELS.toCharArray()) {
            name = name.replace("[" + (int) c + "]", Character.toString(c));
        }

        return name.toLowerCase();
    }
}
