package info.mikaelsvensson.babyname.service.repository.names.request;

public class DefaultNameFacet {
    public boolean returned;

    public DefaultNameFacet returned(boolean returned) {
        this.returned = returned;
        return this;
    }
}
