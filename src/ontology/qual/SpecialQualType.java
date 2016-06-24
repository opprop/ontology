package ontology.qual;

public enum SpecialQualType {

    TOP,
    BOTTOM;

    @Override
    public String toString() {
        switch (this) {
        case TOP: return ""; // TOP is empty
        case BOTTOM: return "ALL"; // using ALL as a special mark that at BOTTOM we have all ontology values
        default: assert false : "only TOP and BOTTOM expected but get: " + this.name();
                 return null;
        }
    }
}
