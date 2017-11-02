#!/bin/bash

ROOT=$(cd $(dirname "$0")/.. && pwd)

CFI="$ROOT"/checker-framework-inference

SOLVER=ontology.solvers.backend.OntologyConstraintSolver

DEBUG_SOVLER=checkers.inference.solver.DebugSolver

IS_HACK=true

CHECKER=ontology.OntologyChecker

# SOLVER="$DEBUG_SOVLER"
# IS_HACK=false

DEBUG_CLASSPATH=""

export CLASSPATH="$ROOT"/ontology/bin:$DEBUG_CLASSPATH:.


$CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --solverArgs="collectStatistic=true,solver=Z3" --hacks="$IS_HACK" -m ROUNDTRIP -afud ./annotated "$@"

# TYPE CHECKING
# $CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --solverArgs="collectStatistic=true,solver=z3" --hacks="$IS_HACK" -m TYPECHECK "$@"