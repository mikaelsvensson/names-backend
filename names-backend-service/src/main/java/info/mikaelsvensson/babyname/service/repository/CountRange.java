package info.mikaelsvensson.babyname.service.repository;

import info.mikaelsvensson.babyname.service.model.Name;

public enum CountRange {
    VERY_POPULAR(50_000, Integer.MAX_VALUE),
    POPULAR(10_000, 50_000),
    NOT_COMMON(1_000, 10_000),
    UNUSUAL(100, 1_000),
    VERY_UNUSUAL(0, 100);

    private final int min;
    private final int max;

    CountRange(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public boolean inRange(Name name) {
        return name.getCount() != null && name.getCount() >= min && name.getCount() < max;
    }
}
