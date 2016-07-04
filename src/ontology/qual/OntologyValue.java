package ontology.qual;

/**
 *
 * @author charleszhuochen
 *
 */

// TODO: this Enum class would be better if it is an inner Enum class of {@code Ontology} annotation because it is a component of {@code Ontology} class
// However, put this class into {@code Ontology} would cause a nullpointer exception in jsr308-langtools/**/{@code JavaCompiler#resolveIdent(String name)}
// the reason of this null pointer exception need to be investigated.
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
