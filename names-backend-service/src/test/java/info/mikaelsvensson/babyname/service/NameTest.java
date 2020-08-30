package info.mikaelsvensson.babyname.service;

import info.mikaelsvensson.babyname.service.model.Name;
import info.mikaelsvensson.babyname.service.util.IdUtils;

import static org.junit.jupiter.api.Assertions.*;

class NameTest {

//    @Test
    void syllableCount() {
        testSyllableCount("Anders",2);
        testSyllableCount("Angelica",4);
        testSyllableCount("Anna",2);
        testSyllableCount("Ann-Charlotte", 4);
        testSyllableCount("Birgitta",3);
        testSyllableCount("Bo", 1);
        testSyllableCount("Benjamin",3);
        testSyllableCount("Carl",1);
        testSyllableCount("Christoffer",3);
        testSyllableCount("Ebba",2);
        testSyllableCount("Elisabet",3);
        testSyllableCount("Emilia",3);
        testSyllableCount("Eva", 2);
        testSyllableCount("Margareta",0);
        testSyllableCount("Marie",0);
        testSyllableCount("Per", 0);
    }

    private void testSyllableCount(String name, int expectedCount) {
        assertEquals(new Name(name, null, null, null, IdUtils.random(), IdUtils.random(), false).syllableCount(), expectedCount);
    }
}