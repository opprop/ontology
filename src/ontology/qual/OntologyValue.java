package ontology.qual;

public enum OntologyValue {

    TOP("TOP"),
    SEQUENCE("sequence"),
    BOTTOM("BOTTOM");

    private String value;

    private OntologyValue(String v) {
        this.value = v;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
