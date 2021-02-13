package info.mikaelsvensson.babyname.service.repository.names.request;

import info.mikaelsvensson.babyname.service.repository.names.FilterVote;

import java.util.Set;

public class VotesNameFacet extends DefaultNameFacet {
    public String selfUserId;
    public String partnerUserId;
    public Set<FilterVote> filterVotes;

    public VotesNameFacet() {
        returned(true);
    }

    public VotesNameFacet selfUserId(String selfUserId) {
        this.selfUserId = selfUserId;
        return this;
    }

    public VotesNameFacet partnerUserId(String partnerUserId) {
        this.partnerUserId = partnerUserId;
        return this;
    }

    public VotesNameFacet filterVotes(Set<FilterVote> filterVotes) {
        this.filterVotes = filterVotes;
        return this;
    }
}
