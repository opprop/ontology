#!/bin/bash

mydir="`dirname $BASH_SOURCE`"
cfDir="${mydir}"/../checker-framework-inference
. "${cfDir}"/scripts/runtime-env-setup.sh

CHECKER=ontology.OntologyChecker

SOLVER=ontology.solvers.backend.OntologySolverEngine
IS_HACK=true

# DEBUG_SOLVER=checkers.inference.solver.DebugSolver
# SOLVER="$DEBUG_SOLVER"
# IS_HACK=false
# DEBUG_CLASSPATH=""

ONTOLOGYPATH=$ROOT/ontology/build/classes/java/main
export CLASSPATH=$ONTOLOGYPATH:$DEBUG_CLASSPATH:.
export external_checker_classpath=$ONTOLOGYPATH

$CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --solverArgs="collectStatistics=true,solver=Z3" --hacks="$IS_HACK" -m ROUNDTRIP -afud ./annotated "$@"

# TYPE CHECKING
# $CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --solverArgs="collectStatistics=true,solver=z3" --hacks="$IS_HACK" -m TYPECHECK "$@"
