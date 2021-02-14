package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Set;

public class FilterVote {
    private final Set<String> userIds;
    private final FilterVoteCondition condition;

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
