package ontology.solvers.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import com.sun.tools.javac.util.Pair;

import checkers.inference.DefaultInferenceResult;
import checkers.inference.InferenceMain;
import checkers.inference.InferenceResult;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.PreferenceConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.model.VariableSlot;
import checkers.inference.solver.backend.Solver;
import checkers.inference.solver.backend.SolverFactory;
import checkers.inference.solver.constraintgraph.ConstraintGraph;
import checkers.inference.solver.constraintgraph.Vertex;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.solver.frontend.LatticeBuilder;
import checkers.inference.solver.frontend.TwoQualifiersLattice;
import checkers.inference.solver.strategy.GraphSolvingStrategy;
import checkers.inference.solver.util.SolverEnvironment;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;

public class OntologyGraphSolvingStrategy extends GraphSolvingStrategy {

    protected ProcessingEnvironment processingEnvironment;

    public OntologyGraphSolvingStrategy(SolverFactory solverFactory) {
        super(solverFactory);
    }

    @Override
    public InferenceResult solve(SolverEnvironment solverEnvironment, Collection<Slot> slots,
            Collection<Constraint> constraints, Lattice lattice) {
        this.processingEnvironment = solverEnvironment.processingEnvironment;
        return super.solve(solverEnvironment, slots, constraints, lattice);
    }

    @Override
    protected List<Solver<?>> separateGraph(SolverEnvironment solverEnvironment, ConstraintGraph constraintGraph,
            Collection<Slot> slots, Collection<Constraint> constraints, Lattice lattice) {
        List<Solver<?>> solvers = new ArrayList<>();

        for (Entry<Vertex, Set<Constraint>> entry : constraintGraph.getConstantPath().entrySet()) {
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

            AnnotationMirror CUR_ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(solverEnvironment.processingEnvironment, ontologyValues);
            TwoQualifiersLattice latticeFor2 = new LatticeBuilder().buildTwoTypeLattice(OntologyUtils.ONTOLOGY_TOP, CUR_ONTOLOGY_BOTTOM);

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
            solvers.add(solverFactory.createSolver(solverEnvironment, reachableSlots, constraints, latticeFor2));
        }
        return solvers;
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
    protected InferenceResult mergeInferenceResults(List<Pair<Map<Integer, AnnotationMirror>, Collection<Constraint>>> inferenceResults) {
        Map<Integer, AnnotationMirror> solutions = new HashMap<> ();
        Map<Integer, EnumSet<OntologyValue>> ontologyResults = new HashMap<> ();

        for (Pair<Map<Integer, AnnotationMirror>, Collection<Constraint>> inferenceResult : inferenceResults) {

            if (inferenceResult.fst == null) {
                return new DefaultInferenceResult(inferenceResult.snd);
            }

            for (Entry<Integer, AnnotationMirror> entry : inferenceResult.fst.entrySet()) {
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

        for (Entry<Integer, EnumSet<OntologyValue>> entry : ontologyResults.entrySet()) {
            EnumSet<OntologyValue> resultValueSet = entry.getValue();
            AnnotationMirror resultAnno = OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                    resultValueSet.toArray(new OntologyValue[resultValueSet.size()]));
            solutions.put(entry.getKey(), resultAnno);
        }

        return new DefaultInferenceResult(solutions);
    }
}
