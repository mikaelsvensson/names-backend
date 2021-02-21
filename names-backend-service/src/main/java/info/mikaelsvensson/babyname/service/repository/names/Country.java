package info.mikaelsvensson.babyname.service.repository.names;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Country {
    SWEDEN("se"),
    USA("us");

    private final String countryCode;

    Country(String countryCode) {
        this.countryCode = countryCode;
    }

    @JsonValue
    public String getCountryCode() {
        return countryCode;
    }
}