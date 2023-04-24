package ontology.solvers.backend;

import checkers.inference.solver.SolverEngine;
import checkers.inference.solver.backend.SolverFactory;
import checkers.inference.solver.backend.z3.Z3Solver;
import checkers.inference.solver.strategy.GraphSolvingStrategy;
import checkers.inference.solver.strategy.SolvingStrategy;
import checkers.inference.solver.util.NameUtils;

import ontology.solvers.backend.z3.OntologyZ3SolverFactory;

public class OntologySolverEngine extends SolverEngine {

    @Override
    protected SolvingStrategy createSolvingStrategy(SolverFactory solverFactory) {
        if (NameUtils.getStrategyName(GraphSolvingStrategy.class).equals(strategyName)) {
            return new OntologyGraphSolvingStrategy(solverFactory);
        } else {
            return super.createSolvingStrategy(solverFactory);
        }
    }

    @Override
    protected SolverFactory createSolverFactory() {
        if (NameUtils.getSolverName(Z3Solver.class).equals(solverName)) {
            return new OntologyZ3SolverFactory();
        } else {
            return super.createSolverFactory();
        }
    }
}
