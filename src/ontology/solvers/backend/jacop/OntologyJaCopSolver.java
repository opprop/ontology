package ontology.solvers.backend.jacop;

import java.util.Collection;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;

import org.checkerframework.framework.type.QualifierHierarchy;

import checkers.inference.model.Constraint;
import checkers.inference.model.Serializer;
import checkers.inference.model.Slot;
import constraintsolver.BackEnd;
import constraintsolver.ConstraintSolver;
import constraintsolver.Lattice;

public class OntologyJaCopSolver extends ConstraintSolver {

    @Override
    protected BackEnd<?, ?> createBackEnd(String backEndType, Map<String, String> configuration, Collection<Slot> slots,
            Collection<Constraint> constraints, QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment, Lattice lattice, Serializer<?, ?> defaultSerializer) {
        return new OntologyJaCopBackend(configuration, slots, constraints, qualHierarchy, processingEnvironment,
                null, lattice);
    }

    @Override
    protected Serializer<?, ?> createSerializer(String value, Lattice lattice) {
        return null;
    }

    @Override
    protected void sanitizeConfiguration() {
        useGraph = false;
    }
}
