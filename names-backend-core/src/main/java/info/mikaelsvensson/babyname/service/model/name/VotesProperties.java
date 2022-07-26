package info.mikaelsvensson.babyname.service.model.name;

public class VotesProperties {
    private final Long selfVoteValue;
    private final Long partnerVoteValue;

    public VotesProperties(Long selfVoteValue, Long partnerVoteValue) {
        this.selfVoteValue = selfVoteValue;
        this.partnerVoteValue = partnerVoteValue;
    }

    public Long getSelfVoteValue() {
        return selfVoteValue;
    }

    public Long getPartnerVoteValue() {
        return partnerVoteValue;
    }
}
