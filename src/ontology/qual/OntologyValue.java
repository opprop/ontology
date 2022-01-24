package ontology.qual;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Enum of possible ontology values. */

// TODO: this Enum class would be better if it is an inner Enum class of {@code Ontology} annotation
// because it is a component of {@code Ontology} class
// However, put this class into {@code Ontology} would cause a nullpointer exception in
// jsr308-langtools/**/{@code JavaCompiler#resolveIdent(String name)}
// the reason of this null pointer exception need to be investigated.
public enum OntologyValue {
    TOP("TOP"),
    SEQUENCE("sequence"),
    DICTIONARY("dictionary"),
    POSITION_3D("position_3d"),
    VELOCITY_3D("velocity_3d"),
    FORCE_3D("force_3d"),
    TORQUE_3D("torque_3d"),
    BOTTOM("BOTTOM");

    private final String value;

    private OntologyValue(String v) {
        this.value = v;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Determine if a given ontology value set is equal to {@link OntologyValue#BOTTOM}.
     *
     * <p>An ontology value set express the same meaning of {@link OntologyValue#BOTTOM} if it: a)
     * Contains value of {@link OntologyValue#BOTTOM}, or b) it contains all real Ontology values.
     *
     * @param ontologyValues agiven ontology value set
     * @return true if the given set is semantically equal to {@link OntologyValue#BOTTOM}
     */
    public static final boolean isEqualToBottom(Set<OntologyValue> ontologyValues) {
        return ontologyValues.contains(BOTTOM)
                || singleRealValueToOrdinal.keySet().equals(ontologyValues);
    }

    /**
     * A cache of the {@link #values()} array. One who use {@code values()} frequently should access
     * this static array instead, to avoid repeatly create newly array when calling {@code
     * values()}.
     */
    public static final OntologyValue[] values = values();

    // TODO: maybe move these two maps to a single Util class for OntologyZ3Serializer?
    // As these two seems special created for the Z3 decoding and encoding.
    /** A map from single real values (i.e. except TOP and BOTTOM) to a zero-based ordinal. */
    public static final Map<OntologyValue, Integer> singleRealValueToOrdinal;

    static {
        Map<OntologyValue, Integer> tempMap = new EnumMap<>(OntologyValue.class);
        int ordinal = 0;
        for (OntologyValue ontologyValue : values) {
            if (ontologyValue == OntologyValue.TOP || ontologyValue == OntologyValue.BOTTOM) {
                continue;
            }
            tempMap.put(ontologyValue, ordinal);
            ordinal += 1;
        }
        singleRealValueToOrdinal = Collections.unmodifiableMap(tempMap);
    }
}
