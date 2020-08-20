package info.mikaelsvensson.babyname.service;

import info.mikaelsvensson.babyname.service.model.NamesCollection;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class NamesCollectionTest {

    @Test
    void getId() {
        IntStream.range(0, 10).forEach((i) -> {
            final var ns = new NamesCollection();
            System.out.println(ns.getId());
        });
    }
}