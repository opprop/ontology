package ontology.solvers.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

import checkers.inference.DefaultInferenceSolution;
import checkers.inference.InferenceSolution;
import checkers.inference.model.Constraint;
import checkers.inference.model.Serializer;
import checkers.inference.model.Slot;
import constraintgraph.ConstraintGraph;
import constraintgraph.Vertex;
import constraintsolver.BackEnd;
import constraintsolver.ConstraintSolver;
import constraintsolver.TwoQualifiersLattice;
import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;
import util.PrintUtils;

public class OntologyConstraintSolver extends ConstraintSolver {

    // TODO: why this processingEnv does not initialted in Constructor?
    private ProcessingEnvironment processingEnvironment;

    @Override
    protected InferenceSolution graphSolve(ConstraintGraph constraintGraph,
            Map<String, String> configuration, Collection<Slot> slots,
            Collection<Constraint> constraints, QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment, Serializer<?, ?> defaultSerializer) {

        this.processingEnvironment = processingEnvironment;
        // TODO: move all Ontology related Annotation creation to OntologyUtils
        AnnotationMirror ONTOLOGY = AnnotationUtils.fromClass(processingEnvironment.getElementUtils(), Ontology.class);
        AnnotationMirror ONTOLOGY_TOP = OntologyUtils.createOntologyAnnotationByValues(processingEnvironment, OntologyValue.TOP);

        // TODO: is using wildcard safe here?
        List<BackEnd<?, ?>> backEnds = new ArrayList<>();
        List<Map<Integer, AnnotationMirror>> inferenceSolutionMaps = new LinkedList<>();

        for (Map.Entry<Vertex, Set<Constraint>> entry : constraintGraph.getConstantPath().entrySet()) {
            AnnotationMirror anno = entry.getKey().getValue();

            if (!AnnotationUtils.areSameIgnoringValues(anno, ONTOLOGY)) {
                continue;
            }

            OntologyValue[] ontologyValues = OntologyUtils.getOntologyValues(anno);

            if (ontologyValues.length == 0 ||
                    //does not solve when the bottom is also TOP
                    EnumSet.copyOf(Arrays.asList(ontologyValues)).contains(OntologyValue.TOP)) {
                continue;
            }

            AnnotationMirror CUR_ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(processingEnvironment, ontologyValues);
            TwoQualifiersLattice latticeFor2 = configureLatticeFor2(ONTOLOGY_TOP, CUR_ONTOLOGY_BOTTOM);
            // TODO: is using wildcard here safe?
            Serializer<?, ?> serializer = createSerializer(backEndType, latticeFor2);
            backEnds.add(createBackEnd(backEndType, configuration, slots, entry.getValue(),
                   qualHierarchy, processingEnvironment, latticeFor2, serializer));
        }

        try {
            if (backEnds.size() > 0) {
                inferenceSolutionMaps = solveInparallel(backEnds);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    return mergeSolution(inferenceSolutionMaps);
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
                }
                ontologyValues.addAll(Arrays.asList(OntologyUtils.getOntologyValues(ontologyAnno)));
            }
        }

        for (Map.Entry<Integer, EnumSet<OntologyValue>> entry : ontologyResults.entrySet()) {
            EnumSet<OntologyValue> resultValueSet = entry.getValue();
            AnnotationMirror resultAnno = OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                    resultValueSet.toArray(new OntologyValue[resultValueSet.size()]));
            result.put(entry.getKey(), resultAnno);
        }

        PrintUtils.printResult(result);
        return new DefaultInferenceSolution(result);
    }
}
