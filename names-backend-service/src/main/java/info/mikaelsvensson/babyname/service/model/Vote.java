package info.mikaelsvensson.babyname.service.model;

public class Vote {
    private String userId;
    private String nameId;
    private VoteType voteType;
    private int voteStrength;

    public Vote() {
    }

    public Vote(String nameId, VoteType voteType) {
        this.nameId = nameId;
        this.voteType = voteType;
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

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public int getVoteStrength() {
        return voteStrength;
    }

    public void setVoteStrength(int voteStrength) {
        this.voteStrength = voteStrength;
    }
}
