package info.mikaelsvensson.babyname.service.repository.names.request;

import java.util.Set;

public class BasicNameFacet extends DefaultNameFacet {
    public Set<String> nameOwnerUserIds;
    public String namePrefix;
    public String nameId;
    public String nameExact;

    public BasicNameFacet() {
        this.returned(true);
    }

    public BasicNameFacet nameOwnerUserIds(Set<String> nameOwnerUserIds) {
        this.nameOwnerUserIds = nameOwnerUserIds;
        return this;
    }

    public BasicNameFacet namePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    public BasicNameFacet nameId(String nameId) {
        this.nameId = nameId;
        return this;
    }

    public BasicNameFacet nameExact(String nameExact) {
        this.nameExact = nameExact;
        return this;
    }
}
