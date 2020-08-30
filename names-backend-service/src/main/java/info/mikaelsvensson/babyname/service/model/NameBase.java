package info.mikaelsvensson.babyname.service.model;

public class NameBase {
    private String id;
    private String ownerUserId;
    protected String name;
    protected Boolean isMale;
    protected Boolean isFemale;
    protected boolean isPublic;

    public NameBase() {
    }

    public NameBase(String name, Boolean isMale, Boolean isFemale, String ownerUserId, String id, boolean isPublic) {
        this.id = id;
        this.name = name;
        this.isMale = isMale;
        this.isFemale = isFemale;
        this.ownerUserId = ownerUserId;
        this.isPublic = isPublic;
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

    public boolean isPublic() {
        return isPublic;
    }

    public int length() {
        return name.length();
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }
}
