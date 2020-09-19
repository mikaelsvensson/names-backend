package info.mikaelsvensson.babyname.service.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameFeatureTest {

    @Test
    void syllableCount() {
        assertSyllableCount("Adam", 2);
        assertSyllableCount("Agnes", 2);
        assertSyllableCount("Agneta", 3);
        assertSyllableCount("Ahmed", 2);
        assertSyllableCount("Albin", 2);
        assertSyllableCount("Alexander", 4);
        assertSyllableCount("Alexandra", 4);
        assertSyllableCount("Alf", 1);
        assertSyllableCount("Ali", 2);
        assertSyllableCount("Ingeborg", 3);
        assertSyllableCount("Ingegerd", 3);
        assertSyllableCount("Ingemar", 3);
        assertSyllableCount("Inger", 2);
        assertSyllableCount("Ingrid", 2);
        assertSyllableCount("Ingvar", 2);
        assertSyllableCount("Irene", 3);
        assertSyllableCount("Jan", 1);
        assertSyllableCount("Maja", 2);
        assertSyllableCount("Malin", 2);
        assertSyllableCount("Marcus", 2);
        assertSyllableCount("Margareta", 4);
        assertSyllableCount("Margaretha", 4);
        assertSyllableCount("Maria", 2);
        assertSyllableCount("Marianne", 3);
        assertSyllableCount("Marie", 2);
        assertSyllableCount("Martin", 2);
        assertSyllableCount("Matilda", 3);
        assertSyllableCount("Mats", 1);
        assertSyllableCount("Mattias", 2);
        assertSyllableCount("Abdirahman", 4);
        assertSyllableCount("Abdirashid", 4);
        assertSyllableCount("Abdirisak", 4);
        assertSyllableCount("Agda", 2);
        assertSyllableCount("Agim", 2);
        assertSyllableCount("Agnar", 2);
        assertSyllableCount("Aneta", 3);
        assertSyllableCount("Anett", 2);
        assertSyllableCount("Angel", 2);
        assertSyllableCount("Anne", 2);
        assertSyllableCount("Ata", 2);
        assertSyllableCount("Atef", 2);
        assertSyllableCount("Athanasios", 4);
        assertSyllableCount("Waldemar", 3);
        assertSyllableCount("Waleed", 2);
        assertSyllableCount("Walentin", 3);
        assertSyllableCount("Wladyslaw", 3);
        assertSyllableCount("Wojciech", 2);
        assertSyllableCount("Einari", 3);
        assertSyllableCount("Eine", 2);
        assertSyllableCount("Eino", 2);
        assertSyllableCount("Emmanuel", 3);
        assertSyllableCount("Emmeli", 3);
        assertSyllableCount("Emmelie", 3);
        assertSyllableCount("Priscilla", 3);
        assertSyllableCount("Przemyslaw", 3);
        assertSyllableCount("Putte", 2);
        assertSyllableCount("Päivi", 2);
        assertSyllableCount("Päivikki", 3);
        assertSyllableCount("Thérése", 3);
        assertSyllableCount("Thérèse", 3);
        assertSyllableCount("Therés", 2);
        assertSyllableCount("Agnieszka", 3);
        assertSyllableCount("Britt Louise", 3);
        assertSyllableCount("Britt-Louise", 3);
        assertSyllableCount("Helinä", 3);
        assertSyllableCount("Ümmügülsüm", 4);
        assertSyllableCount("Étienne", 3);
        assertSyllableCount("Éowyn", 2);
    }

    private void assertSyllableCount(String name, int expected) {
        assertThat(NameFeature.syllableCount(name)).isEqualTo(expected);
    }

    @Test
    void normalize() {
        assertNormalize("ABCabcÅÄÖåäö", "abcabcåäöåäö");
        assertNormalize("éèÜüÉçč", "eeuuecc");
    }

    private void assertNormalize(String name, String expected) {
        assertThat(NameFeature.normalize(name)).isEqualTo(expected);
    }
}