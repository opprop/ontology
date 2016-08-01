#!/bin/bash

ROOT=$(cd $(dirname "$0")/.. && pwd)

CFI="$ROOT"/checker-framework-inference

CF="$ROOT"/checker-framework

# SOLVER=ontology.solvers.classic.OntologySolver
SOLVER=ontology.solvers.backend.OntologyConstraintSolver

DEBUG_SOVLER=checkers.inference.solver.DebugSolver

IS_HACK=true

CHECKER=ontology.OntologyChecker

# SOLVER="$DEBUG_SOVLER"
# IS_HACK=false

CFI_JARS="$CFI"/dist/checker.jar:"$CFI"/dist/plume.jar:"$CFI"/dist/checker-framework-inference.jar

export CLASSPATH="$ROOT"/ontology/bin:"$ROOT"/generic-type-inference-solver/bin

$CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --hacks="$IS_HACK" -m INFER "$@"
