package info.mikaelsvensson.babyname.service.model;

public class Vote {

    public static final Long VALUE_UP = 100L;
    public static final Long VALUE_DOWN = -100L;

    private String userId;
    private String nameId;
    private Long value;

    public Vote() {
    }

    public Vote(String userId, String nameId, Long value) {
        this.userId = userId;
        this.nameId = nameId;
        this.value = value;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNameId() {
        return nameId;
    }

    public void setNameId(String nameId) {
        this.nameId = nameId;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}
