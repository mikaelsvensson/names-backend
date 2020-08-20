package info.mikaelsvensson.babyname.service.model;

import java.util.regex.Pattern;

public class Name extends NameBase {
    private final Integer count;

    public Name(String name, Integer count, Boolean isMale, Boolean isFemale) {
        super(name, isMale, isFemale);
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public int syllableCount() {
        return Pattern.compile("[aeiouyåäöAEIOUYÅÄÖ]+").split(name).length;
    }
}
