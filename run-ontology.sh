#!/bin/bash

ROOT=$(cd $(dirname "$0")/.. && pwd)

CFI=$ROOT/checker-framework-inference

AFU=$ROOT/annotation-tools/annotation-file-utilities
export PATH=$AFU/scripts:$PATH

Z3=$ROOT/z3/bin
export PATH=$Z3:$PATH

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

CFI_LIB=$CFI/lib
export DYLD_LIBRARY_PATH=$CFI_LIB
export LD_LIBRARY_PATH=$CFI_LIB

$CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --solverArgs="collectStatistic=true,solver=Z3" --hacks="$IS_HACK" -m ROUNDTRIP -afud ./annotated "$@"

# TYPE CHECKING
# $CFI/scripts/inference-dev --checker "$CHECKER" --solver "$SOLVER" --solverArgs="collectStatistic=true,solver=z3" --hacks="$IS_HACK" -m TYPECHECK "$@"