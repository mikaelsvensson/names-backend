package info.mikaelsvensson.babyname.service.repository.names;

import java.util.Set;

public class Condition {
    Set<String> userIds;
    String namePrefix;
    int offset;
    int limit;
    Set<String> voteUserIds;
    String nameId;
    String nameExact;
    Set<FilterAttributeNumeric> numericFilters;
    Set<FilterVote> filterVotes;

    public Condition() {
    }

    public Condition userIds(Set<String> value) {
        this.userIds = value;
        return this;
    }

    public Condition namePrefix(String value) {
        this.namePrefix = value;
        return this;
    }

    public Condition offset(int value) {
        this.offset = value;
        return this;
    }

    public Condition limit(int value) {
        this.limit = value;
        return this;
    }

    public Condition voteUserIds(Set<String> value) {
        this.voteUserIds = value;
        return this;
    }

    public Condition nameId(String value) {
        this.nameId = value;
        return this;
    }

    public Condition nameExact(String value) {
        this.nameExact = value;
        return this;
    }

    public Condition numericFilters(Set<FilterAttributeNumeric> value) {
        this.numericFilters = value;
        return this;
    }

    public Condition filterVotes(Set<FilterVote> value) {
        this.filterVotes = value;
        return this;
    }
}
