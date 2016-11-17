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
import java.util.Collections;
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

        InferenceSolution mergedSolution = mergeSolution(inferenceSolutionMaps);

        verifyMergedSolution(mergedSolution, constraints, qualHierarchy, inferenceSolutionMaps);

        return mergedSolution;
    }

    /**
     * verify the merged solution whether is consistent with constraints
     * @param mergedSolution
     * @param constraints
     * @param qualifierHierarchy
     */
    protected void verifyMergedSolution(InferenceSolution mergedSolution,
            Collection<Constraint> constraints, QualifierHierarchy qualifierHierarchy, List<Map<Integer, AnnotationMirror>> solutionMaps) {
        List<ViolatedConsDiagnostic> diagnosticList = new ArrayList<>();

        for (Constraint constraint : constraints) {
            if (!(constraint instanceof SubtypeConstraint)) {
                continue;
            }
            SubtypeConstraint sConstraint = (SubtypeConstraint) constraint;
            Slot subtypeSlot = sConstraint.getSubtype();
            Slot supertypeSlot = sConstraint.getSupertype();
            AnnotationMirror subtype, supertype;

            // currently both suptypeSlot and supertypeSlot should be type of VariableSlot
            assert subtypeSlot instanceof VariableSlot && supertypeSlot instanceof VariableSlot;

            final int subtypeId = ((VariableSlot) subtypeSlot).getId();
            final int supertypeId = ((VariableSlot) supertypeSlot).getId();

            if (subtypeSlot instanceof ConstantSlot && supertypeSlot instanceof ConstantSlot) {
                continue;

            } else if (subtypeSlot instanceof ConstantSlot) {
                subtype = ((ConstantSlot) subtypeSlot).getValue();
                supertype= mergedSolution.getAnnotation(supertypeId);

                assert subtype != null;

                if (supertype == null) {
                    logNoSolution(sConstraint, subtype, supertype);
                    continue;
                }

            } else if (supertypeSlot instanceof ConstantSlot) {
                subtype = mergedSolution.getAnnotation(subtypeId);
                supertype = ((ConstantSlot) supertypeSlot).getValue();

                assert supertype != null;

                if (subtype == null) {
                    logNoSolution(sConstraint, subtype, supertype);
                    continue;
                }

            } else {
                subtype = mergedSolution.getAnnotation(subtypeId);
                supertype = mergedSolution.getAnnotation(supertypeId);

                if (subtype == null || supertype == null) {
                    logNoSolution(sConstraint, subtype, supertype);
                    continue;
                }
            }

            assert subtype != null && supertype != null;

            if (!qualifierHierarchy.isSubtype(subtype, supertype)) {
                ViolatedConsDiagnostic consDiagRes = new ViolatedConsDiagnostic(sConstraint, subtype, supertype);

                List<AnnotationMirror> subtypeSolutions = new ArrayList<> ();
                List<AnnotationMirror> supertypeSolutions = new ArrayList<> ();

                for (Map<Integer, AnnotationMirror> solutionMap : solutionMaps) {
                    if (solutionMap.containsKey(subtypeId)) {
                        subtypeSolutions.add(solutionMap.get(subtypeId));
                    }
                    if (solutionMap.containsKey(supertypeId)) {
                        supertypeSolutions.add(solutionMap.get(supertypeId));
                    }
                }

                consDiagRes.setSubtypeSolutions(subtypeSolutions);
                consDiagRes.setSupertypeSolutions(supertypeSolutions);
                diagnosticList.add(consDiagRes);
            }
        }

        if (!diagnosticList.isEmpty()) {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("solved solution doesn't consistent with below constraints: \n");
            for (ViolatedConsDiagnostic result : diagnosticList) {
                sBuilder.append(result + "\n");
            }
            ErrorReporter.errorAbort(sBuilder.toString());
        }
    }

    private void logNoSolution(SubtypeConstraint subtypeConstraint, AnnotationMirror subtype, AnnotationMirror supertype) {
        InferenceMain.getInstance().logger.warning("no solution for subtype constraint: " + subtypeConstraint +
                "\tinferred subtype: " + subtype + "\tinferred supertype: " + supertype);
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
        OntologyStatisticUtil.writeInferenceResult("ontology-inferred-slots-statistic.txt", result);
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

    protected class ViolatedConsDiagnostic {
        SubtypeConstraint subtypeConstraint;
        AnnotationMirror inferredSubtype;
        AnnotationMirror inferredSupertype;
        List<AnnotationMirror> subtypeSolutions;
        List<AnnotationMirror> supertypeSolutions;

        public ViolatedConsDiagnostic(SubtypeConstraint subtypeConstraint,
                AnnotationMirror subtype, AnnotationMirror supertype) {
            this.subtypeConstraint = subtypeConstraint;
            this.inferredSubtype = subtype;
            this.inferredSupertype = supertype;
            subtypeSolutions = Collections.emptyList();
            supertypeSolutions = Collections.emptyList();
        }

        public void setSubtypeSolutions(List<AnnotationMirror> subtypeSolutions) {
            this.subtypeSolutions = subtypeSolutions;
        }

        public void setSupertypeSolutions(List<AnnotationMirror> supertypeSolutions) {
            this.supertypeSolutions = supertypeSolutions;
        }

        @Override
        public String toString() {
            return subtypeConstraint
                    + "\tsubtype: " + inferredSubtype
                    + "\tsupertype: " + inferredSupertype
                    + "\t\n subtypeSolutions: " + subtypeSolutions
                    + "\t\n supertypeSolutions: " + supertypeSolutions;
        }
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
