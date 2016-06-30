package ontology.solvers.classic;

import java.util.HashMap;
import java.util.Map;

import ontology.qual.OntologyValue;

public class SequenceSolution {
    private final Map<Integer, Boolean> result;
    private final OntologyValue value;

    public SequenceSolution(Map<Integer, Boolean> result, OntologyValue value) {
        this.result = result;
        this.value = value;
    }

    private SequenceSolution(OntologyValue value) {
        this(new HashMap<Integer, Boolean>(), value);
    }

    public Map<Integer, Boolean> getResult() {
        return result;
    }

    public OntologyValue getValue() {
        return value;
    }

    public static SequenceSolution noSolution(OntologyValue value) {
        return new SequenceSolution(value);
    }

}
