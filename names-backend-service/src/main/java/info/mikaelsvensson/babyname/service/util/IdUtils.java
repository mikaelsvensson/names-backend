package info.mikaelsvensson.babyname.service.util;

import java.util.Random;

public class IdUtils {
    private static Random random = new Random();

    public static String random() {
        return random.ints('a', 'z' + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
