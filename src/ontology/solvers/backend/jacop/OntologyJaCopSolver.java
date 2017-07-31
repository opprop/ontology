package ontology.solvers.backend.jacop;

import java.util.Collection;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.framework.type.QualifierHierarchy;

import checkers.inference.DefaultInferenceSolution;
import checkers.inference.InferenceSolution;
import checkers.inference.model.Constraint;
import checkers.inference.model.Serializer;
import checkers.inference.model.Slot;
import constraintsolver.BackEnd;
import constraintsolver.ConstraintSolver;
import constraintsolver.Lattice;
import ontology.util.OntologyStatisticUtil;
import util.PrintUtils;

public class OntologyJaCopSolver extends ConstraintSolver {

    @Override
    protected BackEnd<?, ?> createBackEnd(String backEndType, Map<String, String> configuration, Collection<Slot> slots,
            Collection<Constraint> constraints, QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment, Lattice lattice, Serializer<?, ?> defaultSerializer) {
        return new OntologyJaCopBackend(configuration, slots, constraints, qualHierarchy, processingEnvironment,
                null, lattice);
    }

    @Override
    protected InferenceSolution solve() {
        Map<Integer, AnnotationMirror> result = realBackEnd.solve();
        PrintUtils.printResult(result);
        OntologyStatisticUtil.writeInferenceResult("ontology-infer-result.txt", result);
        return new DefaultInferenceSolution(result);
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
