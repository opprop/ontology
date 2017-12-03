#!/bin/bash

set -e

WORKING_DIR=$(pwd)

if [ -z "${JSR308}" ] ; then
    export JSR308=$(cd $(dirname "$0")/.. && pwd)
fi

DLJC="$JSR308"/do-like-javac
export AFU="$JSR308"/annotation-tools/annotation-file-utilities
export PATH="$PATH":"$AFU"/scripts

export CLASSPATH="$JSR308"/ontology/bin

export DYLD_LIBRARY_PATH="$JSR308"/checker-framework-inference/lib
export LD_LIBRARY_PATH="$JSR308"/checker-framework-inference/lib


CHECKER=ontology.OntologyChecker
SOLVER=ontology.solvers.backend.OntologySolverEngine

#parsing build command of the target program
build_cmd="$1"
shift
while [ "$#" -gt 0 ]
do
    build_cmd="$build_cmd $1"
    shift
done

cd "$WORKING_DIR"

infer_cmd="python $DLJC/dljc -t inference --guess --crashExit --checker $CHECKER --solver $SOLVER --solverArgs=\"collectStatistic=true,solver=Z3\" -o logs -m INFER -afud $WORKING_DIR/annotated -- $build_cmd "

# debug_onlyCompile="--onlyCompileBytecodeBase true"
debug_cmd="python $DLJC/dljc -t testminimizer --annotationClassPath $JSR308/ontology/bin $debug_onlyCompile --expectOutputRegex 'Z3 Unsatisfiable' --checker $DATAFLOW_CHECKER --solver $DATAFLOW_SOLVER --solverArgs=\"collectStatistic=true,solver=Z3\" -o logs -m INFER -afud $WORKING_DIR/annotated -- $build_cmd "


running_cmd=$infer_cmd


echo "============ Important variables ============="
echo "JSR308: $JSR308"
echo "CLASSPATH: $CLASSPATH"
echo "build cmd: $build_cmd"
echo "running cmd: $running_cmd"
echo "============================================="

eval "$running_cmd"

echo "---- Reminder: do not forget to clean up the project! ----"
