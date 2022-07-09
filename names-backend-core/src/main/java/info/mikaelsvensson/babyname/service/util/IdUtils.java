package info.mikaelsvensson.babyname.service.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IdUtils {
    private static final Random random = new Random();

    private static final String A_TO_Z = IntStream.range('a', 'z' + 1).mapToObj(Character::toString).collect(Collectors.joining());

    public static final List<Character> CHARS = A_TO_Z
            .chars() // Convert to an IntStream
            .mapToObj(i -> (char) i) // Convert int to char, which gets boxed to Character
            .collect(Collectors.toList()); // Collect in a List<Character>


    public static String random() {
        return random.ints('a', 'z' + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String randomAlphabet(long seed) {
        final var chars = new ArrayList<>(CHARS);
        if (-1 == seed) {
            Collections.shuffle(chars);
        } else {
            Collections.shuffle(chars, new Random(seed));
        }
        return StringUtils.join(chars, "");
    }
}
