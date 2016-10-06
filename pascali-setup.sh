#!/bin/bash

# Fail the whole script if any command fails
set -e

export SHELLOPTS

export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(dirname $(readlink -f $(/usr/bin/which java)))))}

export JSR308=$ROOT

export REPO_SITE=opprop

CUR_DIR=$(pwd)
JSR308=$(cd ../ && pwd)

##### build checker-framework
if [-d $JSR308/checker-framework] ; then
    (cd $JSR308/checker-framework && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/checker-framework.git)
fi

## copy from checker-framework/.travis-build-without-test.sh

## Build annotation-tools (Annotation File Utilities)
if [ -d $JSR308/annotation-tools ] ; then
    # Older versions of git don't support the -C command-line option
    (cd ../annotation-tools && git pull)
else
    (cd .. && git clone --depth 1 https://github.com/"$REPO_SITE"/annotation-tools.git)
fi
# This also builds jsr308-langtools
(cd $JSR308/annotation-tools/ && ./.travis-build-without-test.sh)

## copy from checker-framework/.travis-build-without-test.sh end

## try only build checker-framework, with jdk
ant -f $JSR308/checker-framework/checker/ dist

##### build checker-framework-inference
if [-d $JSR308/checker-framework-inference] ; then
    (cd $JSR308/checker-framework-inference && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/checker-framework-inference.git)
fi

(cd $JSR308/checker-framework-inference && gradle dist)

##### build generic-type-inference-solver
if [-d $JSR308/generic-type-inference-solver] ; then
    (cd $JSR308/generic-type-inference-solver && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/generic-type-inference-solver.git)
fi

(cd $JSR308/generic-type-inference-solver/scripts && ./compile.sh)

##### build ontology
(cd $JSR308/ontology && gradle build)