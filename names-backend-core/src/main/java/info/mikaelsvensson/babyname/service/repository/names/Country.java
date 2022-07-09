package info.mikaelsvensson.babyname.service.repository.names;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Country {
    SWEDEN("se"),
    NORWAY("no"),
    USA("us"),
    DENMARK("dk"),
    FINLAND("fi");

    private final String countryCode;

    Country(String countryCode) {
        this.countryCode = countryCode;
    }

    @JsonValue
    public String getCountryCode() {
        return countryCode;
    }
}
