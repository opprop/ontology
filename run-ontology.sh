#!/bin/bash

ROOT=$(cd $(dirname "$0")/.. && pwd)

CFI="$ROOT"/checker-framework-inference

# SOLVER=ontology.solvers.classic.OntologySolver
SOLVER=ontology.solvers.backend.OntologyConstraintSolver

SET_SOLVER=ontology.solvers.backend.jacop.OntologyJaCopSolver

Z3_SOLVER=ontology.solvers.backend.z3.OntologyZ3Solver

DEBUG_SOVLER=checkers.inference.solver.DebugSolver

IS_HACK=true

CHECKER=ontology.OntologyChecker

# CHECKER=dataflow.DataflowChecker

# SOLVER=dataflow.solvers.general.DataflowGeneralSolver

# SOLVER="$DEBUG_SOVLER"
# IS_HACK=false

DEBUG_CLASSPATH=/Users/charleszhuochen/Programming/UWaterloo/jsr308/3dProjects/jReactPhysics3D/inferDebugBuild

export CLASSPATH="$ROOT"/ontology/bin:"$DEBUG_CLASSPATH":.

export DYLD_LIBRARY_PATH=${ROOT}/z3/build

$CFI/scripts/inference-dev --checker "$CHECKER" --solver "$Z3_SOLVER" --solverArgs="collectStatistic=true" --hacks="$IS_HACK" -m ROUNDTRIP -afud ./annotated "$@"

# TYPE CHECKING
# $CFI/scripts/inference-dev --checker "$CHECKER" --solver "$Z3_SOLVER" --solverArgs="collectStatistic=true" --hacks="$IS_HACK" -m TYPECHECK "$@"