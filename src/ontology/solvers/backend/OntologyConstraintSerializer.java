package ontology.solvers.backend;

import checkers.inference.model.Serializer;
import constraintsolver.ConstraintSerializer;
import constraintsolver.Lattice;

public class OntologyConstraintSerializer<S, T> extends ConstraintSerializer<S, T> {

    @SuppressWarnings("unchecked")
    public OntologyConstraintSerializer(String backEndType, Lattice lattice) {
        super(backEndType, lattice);
        if (backEndType.equals("maxsatbackend.Lingeling") || backEndType.equals("maxsatbackend.MaxSat")) {
            realSerializer = (Serializer<S, T>) new OntologyMaxsatSerializer(lattice);
        }
    }
}
