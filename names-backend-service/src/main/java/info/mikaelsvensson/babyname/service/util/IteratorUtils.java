package info.mikaelsvensson.babyname.service.util;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IteratorUtils {
    public static <T> Stream<T> toStream(Iterator<T> iterator) {
        try {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE), false);
        } catch (Exception exception) {
            return Stream.empty();
        }
    }

    public static <T> List<T> toList(Iterator<T> iterator) {
        try {
            return StreamSupport
                    .stream(((Iterable<T>) () -> iterator).spliterator(), false)
                    .collect(Collectors.toList());
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }
}
