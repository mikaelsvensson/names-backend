package info.mikaelsvensson.babyname.service.util;

import java.util.ArrayList;
import java.util.List;

public class Ngrams {
    public static List<String> get(String input, boolean isUnigramConsidered) {
        return get(input, isUnigramConsidered ? new int[]{1, 2, 3} : new int[]{2, 3}, true);
    }

    public static List<String> get(String input, int[] lengths, boolean addStartEndIndicators) {
        final var strings = new ArrayList<String>();
        for (int substringLength : lengths) {
            for (int pos = 0; pos < input.length() - substringLength + 1; pos++) {
                strings.add(
                        (addStartEndIndicators ? (pos > 0 ? "*" : " ") : "") +
                                input.substring(pos, pos + substringLength) +
                                (addStartEndIndicators ? (pos < input.length() - substringLength ? "*" : " ") : "")
                );
            }
        }
        return strings;
    }
}
