package info.mikaelsvensson.babyname.service.model;

import info.mikaelsvensson.babyname.service.util.IdUtils;

public class NameBase {
    private final String id;
    protected final String name;
    protected final Boolean isMale;
    protected final Boolean isFemale;

    public NameBase(String name, Boolean isMale, Boolean isFemale) {
        this.id = IdUtils.random();
        this.name = name;
        this.isMale = isMale;
        this.isFemale = isFemale;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean isMale() {
        return isMale;
    }

    public Boolean isFemale() {
        return isFemale;
    }

    public int length() {
        return name.length();
    }
}
