package info.mikaelsvensson.babyname.service.model;

import java.util.regex.Pattern;

public class Name extends NameBase {
    private final Integer count;

    public Name(String name, Integer count, Boolean isMale, Boolean isFemale, String ownerUserId, String id, boolean isPublic) {
        super(name, isMale, isFemale, ownerUserId, id, isPublic);
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public int syllableCount() {
        return Pattern.compile("[aeiouyåäöAEIOUYÅÄÖ]+").split(name).length;
    }
}
