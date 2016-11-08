package ontology.solvers.classic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;

import checkers.inference.InferenceMain;
import checkers.inference.SlotManager;
import checkers.inference.model.Constraint;
import ontology.qual.OntologyValue;

public class SequenceSolver {
    private final SlotManager slotManager;
    private final OntologyValue value;
    private final OntologySerializer serializer;
    private final List<VecInt> clauses;

    public SequenceSolver(OntologyValue ontologyValue, Collection<Constraint> constraints, OntologySerializer serializer) {
        this.value = ontologyValue;
        this.serializer = serializer;
        this.slotManager = InferenceMain.getInstance().getSlotManager();
        this.clauses = convertToCNF(constraints);
        // writeCNF();
    }

    private List<VecInt> convertToCNF(Collection<Constraint> constraints) {
        return serializer.convertAll(constraints);
    }

    public SequenceSolution solve() {
        Map<Integer, Boolean> idToExistence = new HashMap<>();
        Map<Integer, Boolean> result = new HashMap<>();

        final int totalVars = slotManager.getNumberOfSlots();
        final int totalClauses = clauses.size();

        try {

            final WeightedMaxSatDecorator solver = new WeightedMaxSatDecorator(org.sat4j.pb.SolverFactory.newBoth());

            solver.newVar(totalVars);
            solver.setExpectedNumberOfClauses(totalClauses);
            //Arbitrary timeout
            solver.setTimeoutMs(1000000);
            for (VecInt clause : clauses) {
                solver.addSoftClause(clause);
            }

            boolean hasSolution = solver.isSatisfiable();

            if (hasSolution) {

                final Map<Integer, Integer> existentialToPotentialIds = serializer.getExistentialToPotentialVar();
                int[] solution = solver.model();
                for (Integer var : solution) {
                    boolean varIsTrue = var > 0;
                    var = Math.abs(var);
                    Integer potential = existentialToPotentialIds.get(var);
                    if (potential != null) {
                        idToExistence.put(potential, varIsTrue);
                    } else {
                        result.put(var, !varIsTrue);
                    }
                }
                return new SequenceSolution(result, value);
            }

        } catch (Throwable th) {
            VecInt lastClause = clauses.get(clauses.size() - 1);
            throw new RuntimeException("Error MAX-SAT solving! " + lastClause, th);
        }

        return SequenceSolution.noSolution(value);
    }
}
