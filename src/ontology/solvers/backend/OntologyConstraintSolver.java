package ontology.solvers.backend;

import ontology.qual.OntologyValue;
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

import util.PrintUtils;
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
import constraintgraph.ConstraintGraph;
import constraintgraph.GraphBuilder;
import constraintgraph.GraphBuilder.SubtypeDirection;
import constraintgraph.Vertex;
import constraintsolver.BackEnd;
import constraintsolver.ConstraintSolver;
import constraintsolver.Lattice;
import constraintsolver.TwoQualifiersLattice;

public class OntologyConstraintSolver extends ConstraintSolver {

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
            TwoQualifiersLattice latticeFor2 = configureLatticeFor2(OntologyUtils.ONTOLOGY_TOP, CUR_ONTOLOGY_BOTTOM);

            Set<Constraint> consSet = entry.getValue();
            Slot vertixSlot = entry.getKey().getSlot();
            if (!(vertixSlot instanceof ConstantSlot)) {
                ErrorReporter.errorAbort("vertixSlot should be constantslot!");
            }

            addPreferenceToCurBottom((ConstantSlot) entry.getKey().getSlot(), consSet);
            // TODO: is using wildcard here safe?
            Serializer<?, ?> serializer = createSerializer(backEndType, latticeFor2);
            backEnds.add(createBackEnd(backEndType, configuration, slots, consSet,
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
    protected Serializer<?, ?> createSerializer(String value, Lattice lattice) {
        return new OntologyConstraintSerializer<>(value, lattice);
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
        result = inferMissingConstraint(result);
        PrintUtils.printResult(result);
        return new DefaultInferenceSolution(result);
    }

    @Override
    protected Map<Integer, AnnotationMirror> inferMissingConstraint(Map<Integer, AnnotationMirror> result) {
        Collection<Constraint> missingConstraints = this.constraintGraph.getMissingConstraints();
        for (Constraint constraint : missingConstraints) {
            if (constraint instanceof SubtypeConstraint) {
                SubtypeConstraint subtypeConstraint = (SubtypeConstraint) constraint;
                if (!(subtypeConstraint.getSubtype() instanceof ConstantSlot)
                        && !(subtypeConstraint.getSupertype() instanceof ConstantSlot)) {
                    VariableSlot subtype = (VariableSlot) subtypeConstraint.getSubtype();
                    VariableSlot supertype = (VariableSlot) subtypeConstraint.getSupertype();
                    if (result.keySet().contains(supertype.getId())) {
                        AnnotationMirror anno = result.get(supertype.getId());
                        OntologyValue[] ontologyValues = OntologyUtils.getOntologyValues(anno);
                        if (!(ontologyValues.length == 0 || EnumSet
                                .copyOf(Arrays.asList(ontologyValues)).contains(OntologyValue.TOP))) {
                            result.put(subtype.getId(), anno);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected ConstraintGraph generateGraph(Collection<Slot> slots, Collection<Constraint> constraints) {
        GraphBuilder graphBuilder = new GraphBuilder(slots, constraints, SubtypeDirection.FROMSUBTYPE);
        ConstraintGraph constraintGraph = graphBuilder.buildGraph();
        return constraintGraph;
    }
}
