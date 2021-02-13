package info.mikaelsvensson.babyname.service.model.name;

public class VotesProperties {
    private final Integer selfVoteValue;
    private final Integer partnerVoteValue;

    public VotesProperties(Integer selfVoteValue, Integer partnerVoteValue) {
        this.selfVoteValue = selfVoteValue;
        this.partnerVoteValue = partnerVoteValue;
    }

    public Integer getSelfVoteValue() {
        return selfVoteValue;
    }

    public Integer getPartnerVoteValue() {
        return partnerVoteValue;
    }
}
