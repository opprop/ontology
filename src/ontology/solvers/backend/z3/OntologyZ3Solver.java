package ontology.solvers.backend.z3;

import checkers.inference.solver.SolverEngine;
import checkers.inference.solver.backend.FormatTranslator;
import checkers.inference.solver.backend.SolverType;
import checkers.inference.solver.frontend.Lattice;

public class OntologyZ3Solver extends SolverEngine {

//    @Override
//    protected void postStatistic(Collection<Constraint> constraints, Collection<Slot> slots,
//            DefaultInferenceSolution inferResult) {
//        OntologyStatisticUtil.writeInferenceResult("ontology-infer-result.txt", inferResult.getResultMap());
//    }


//    @Override
//    protected void postVerification(Collection<Constraint> constraints, Collection<Slot> slots,
//            Map<Integer, AnnotationMirror> inferResult, QualifierHierarchy qualifierHierarchy) {
//        List<Map<Integer, AnnotationMirror>> solutionMaps = new ArrayList<>();
//        solutionMaps.add(inferResult);
//        OntologyStatisticUtil.verifySolution(inferResult, constraints, qualifierHierarchy, solutionMaps);
//        
//    }

    @Override
    protected FormatTranslator<?, ?, ?> createFormatTranslator(SolverType backEndType, Lattice lattice) {
        return new OntologyZ3FormatTranslator(lattice);
    }

    @Override
    protected void sanitizeConfiguration() {
        useGraph = false;
    }
}
