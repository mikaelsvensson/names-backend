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

    public static Country fromCountryCode(String countryCode) {
        for (Country value : values()) {
            if (value.countryCode.equals(countryCode)) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("%s is not a value country code.", countryCode));
    }
}
