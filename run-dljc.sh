#!/bin/bash

WORKING_DIR=$(pwd)

if [ -z "${JSR308}" ] ; then
    export JSR308=$(cd $(dirname "$0")/.. && pwd)
fi

DLJC="$JSR308"/do-like-javac

export AFU="$JSR308"/annotation-tools/annotation-file-utilities
export PATH="$PATH":"$AFU"/scripts

export CLASSPATH="$JSR308"/ontology/bin:"$JSR308"/generic-type-inference-solver/bin:"$JSR308"/jacop/target/jacop-4.4.0.jar

SOLVER=ontology.solvers.backend.OntologyConstraintSolver
SET_SOLVER=ontology.solvers.backend.jacop.OntologyJaCopSolver

#parsing build command of the target program
build_cmd="$1"
shift
while [ "$#" -gt 0 ]
do
    build_cmd="$build_cmd $1"
    shift
done

cd "$WORKING_DIR"
# running_cmd="python $DLJC/dljc -t inference --checker ontology.OntologyChecker --solver ontology.solvers.backend.OntologyConstraintSolver --solverArgs=\"backEndType=maxsatbackend.Lingeling,solveInParallel=false\" -o logs -m ROUNDTRIP -afud $WORKING_DIR/annotated -- $build_cmd"
infer_cmd="python $DLJC/dljc -t inference --checker ontology.OntologyChecker --solver $SET_SOLVER --solverArgs=\"collectStatistic=true\" -o logs -m ROUNDTRIP -afud $WORKING_DIR/annotated -- $build_cmd "

# debug_onlyCompile="--onlyCompileBytecodeBase true"
debug_cmd="python $DLJC/dljc -t inference_debug --annotationClassPath $JSR308/ontology/bin $debug_onlyCompile --checker ontology.OntologyChecker --solver $SET_SOLVER --solverArgs=\"collectStatistic=true\" -o logs -m INFER -afud $WORKING_DIR/annotated -- $build_cmd "


running_cmd=$debug_cmd

# running_cmd=$debug_cmd

echo "============ Important variables ============="
echo "JSR308: $JSR308"
echo "CLASSPATH: $CLASSPATH"
echo "build cmd: $build_cmd"
echo "running cmd: $running_cmd"
echo "============================================="

eval "$running_cmd"

echo "---- Reminder: do not forget to clean up the project! ----"
