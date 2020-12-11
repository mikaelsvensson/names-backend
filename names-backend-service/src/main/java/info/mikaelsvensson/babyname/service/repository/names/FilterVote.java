package info.mikaelsvensson.babyname.service.repository.names;

import java.util.Set;

public class FilterVote {
    private Set<String> userIds;
    private FilterVoteCondition condition;

    public FilterVote(Set<String> userIds, FilterVoteCondition condition) {
        this.userIds = userIds;
        this.condition = condition;
    }

    public Set<String> getUserIds() {
        return userIds;
    }

    public FilterVoteCondition getCondition() {
        return condition;
    }
}
