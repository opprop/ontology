package ontology.solvers.backend;

import ontology.qual.OntologyValue;
import ontology.util.OntologyStatisticUtil;
import ontology.util.OntologyUtils;

import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.inference.DefaultInferenceSolution;
import checkers.inference.InferenceMain;
import checkers.inference.InferenceSolution;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.PreferenceConstraint;
import checkers.inference.model.Serializer;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.model.VariableSlot;
import checkers.inference.solver.GeneralSolver;
import checkers.inference.solver.backend.BackEnd;
import checkers.inference.solver.backend.BackEndType;
import checkers.inference.solver.constraintgraph.ConstraintGraph;
import checkers.inference.solver.constraintgraph.Vertex;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.solver.frontend.TwoQualifiersLattice;

public class OntologyConstraintSolver extends GeneralSolver {

    // TODO: why this processingEnv does not initialted in Constructor?
    private ProcessingEnvironment processingEnvironment;

    @Override
    protected InferenceSolution graphSolve(ConstraintGraph constraintGraph,
            Map<String, String> configuration, Collection<Slot> slots,
            Collection<Constraint> constraints, QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment, Serializer<?, ?> defaultSerializer) {
        this.processingEnvironment = processingEnvironment;
        // TODO: is using wildcard safe here?
        List<BackEnd<?, ?>> backEnds = new ArrayList<>();
        List<Map<Integer, AnnotationMirror>> inferenceSolutionMaps = new LinkedList<>();

        for (Map.Entry<Vertex, Set<Constraint>> entry : constraintGraph.getConstantPath().entrySet()) {
            AnnotationMirror anno = entry.getKey().getValue();
            if (!AnnotationUtils.areSameIgnoringValues(anno, OntologyUtils.ONTOLOGY)) {
                continue;
            }

            OntologyValue[] ontologyValues = OntologyUtils.getOntologyValues(anno);

            if (ontologyValues.length == 0 ||
                    //does not solve when the bottom is also TOP
                    EnumSet.copyOf(Arrays.asList(ontologyValues)).contains(OntologyValue.TOP)) {
                continue;
            }

            AnnotationMirror CUR_ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(processingEnvironment, ontologyValues);
            TwoQualifiersLattice latticeFor2 = createTwoQualifierLattice(OntologyUtils.ONTOLOGY_TOP, CUR_ONTOLOGY_BOTTOM);

            Set<Constraint> consSet = entry.getValue();
            Slot vertixSlot = entry.getKey().getSlot();
            if (!(vertixSlot instanceof ConstantSlot)) {
                ErrorReporter.errorAbort("vertixSlot should be constantslot!");
            }

            Set<Slot> reachableSlots = new HashSet<>();
            for (Constraint constraint : consSet) {
                reachableSlots.addAll(constraint.getSlots());
            }

            addPreferenceToCurBottom((ConstantSlot) entry.getKey().getSlot(), consSet);
            // TODO: is using wildcard here safe?
            Serializer<?, ?> serializer = createSerializer(backEndType, latticeFor2);
            backEnds.add(createBackEnd(backEndType, configuration, reachableSlots, consSet,
                   qualHierarchy, processingEnvironment, latticeFor2, serializer));
        }

        try {
            if (backEnds.size() > 0) {
                inferenceSolutionMaps = solveInparallel(backEnds);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        InferenceSolution mergedSolution = mergeSolution(inferenceSolutionMaps);

        try {
            OntologyStatisticUtil.verifySolution(mergedSolution, constraints, qualHierarchy, inferenceSolutionMaps);
        } finally {
            if (collectStatistic) {
                OntologyStatisticUtil.getPostVerifyStatRecorder().writeStatistic();
            }
        }

        return mergedSolution;
    }


    private void addPreferenceToCurBottom(ConstantSlot curBtm, Set<Constraint> consSet) {
        Set<Constraint> preferSet = new HashSet<>();
        for (Constraint constraint : consSet) {
            if (constraint instanceof SubtypeConstraint) {
                SubtypeConstraint subCons = (SubtypeConstraint) constraint;
                Slot superType = subCons.getSupertype();
                if (superType instanceof ConstantSlot) {
                    continue;
                }

               PreferenceConstraint preferCons = InferenceMain.getInstance().getConstraintManager()
               .createPreferenceConstraint((VariableSlot) superType, curBtm, 50);
               preferSet.add(preferCons);
            }
        }
        consSet.addAll(preferSet);
    }
    @Override
    protected Serializer<?, ?> createSerializer(BackEndType backEndType, Lattice lattice) {
        switch (backEndType) {
        case MAXSAT:
        case LINGELING: {
            return new OntologyMaxsatSerializer(lattice);
        }

        default:
            return backEndType.createDefaultSerializer(lattice);
        }
    }

    @Override
    protected InferenceSolution mergeSolution(List<Map<Integer, AnnotationMirror>> inferenceSolutionMaps) {
        Map<Integer, AnnotationMirror> result = new HashMap<> ();
        Map<Integer, EnumSet<OntologyValue>> ontologyResults = new HashMap<> ();

        for (Map<Integer, AnnotationMirror> inferenceSolutionMap : inferenceSolutionMaps) {
            for (Map.Entry<Integer, AnnotationMirror> entry : inferenceSolutionMap.entrySet()) {
                Integer id = entry.getKey();
                AnnotationMirror ontologyAnno = entry.getValue();
                EnumSet<OntologyValue> ontologyValues = ontologyResults.get(id);
                if (ontologyValues == null) {
                    ontologyValues = EnumSet.noneOf(OntologyValue.class);
                    ontologyResults.put(id, ontologyValues);
                    ontologyValues.addAll(Arrays.asList(OntologyUtils.getOntologyValues(ontologyAnno)));
                    continue;
                }
                EnumSet<OntologyValue> annoValues = EnumSet.noneOf(OntologyValue.class);
                annoValues.addAll(Arrays.asList(OntologyUtils.getOntologyValues(ontologyAnno)));

                EnumSet<OntologyValue> lub = OntologyUtils.lubOfOntologyValues(ontologyValues, annoValues);
                ontologyValues.clear();
                ontologyValues.addAll(lub);
            }
        }

        for (Map.Entry<Integer, EnumSet<OntologyValue>> entry : ontologyResults.entrySet()) {
            EnumSet<OntologyValue> resultValueSet = entry.getValue();
            AnnotationMirror resultAnno = OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                    resultValueSet.toArray(new OntologyValue[resultValueSet.size()]));
            result.put(entry.getKey(), resultAnno);
        }

        if (collectStatistic) {
            OntologyStatisticUtil.writeInferenceResult("ontology-inferred-slots-statistic.txt", result);
        }

        return new DefaultInferenceSolution(result);
    }

    @Override
    protected void sanitizeConfiguration() {
        if (!useGraph) {
            useGraph = true;
            InferenceMain.getInstance().logger.warning("OntologyConstraintSolver: Don't use graph to solve constraints will "
                    + "cause wrong answers in Ontology type system. Modified solver argument \"useGraph\" to true.");
        }
    }
}
