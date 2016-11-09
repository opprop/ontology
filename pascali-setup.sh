#!/bin/bash

# Fail the whole script if any command fails
set -e

. ./env-setup.sh

export SHELLOPTS

#default value is pascaliUWat. REPO_SITE may be set to other value for travis test purpose.
export REPO_SITE="${REPO_SITE:-pascaliUWat}"

echo "------ Downloading everthing from REPO_SITE: $REPO_SITE ------"

CUR_DIR=$(pwd)

##### build checker-framework
if [ -d $JSR308/checker-framework ] ; then
    (cd $JSR308/checker-framework && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/checker-framework.git)
fi

## Build annotation-tools (Annotation File Utilities)
if [ -d $JSR308/annotation-tools ] ; then
    # Older versions of git don't support the -C command-line option
    (cd ../annotation-tools && git pull)
else
    (cd .. && git clone --depth 1 https://github.com/"$REPO_SITE"/annotation-tools.git)
fi
# This also builds jsr308-langtools
(cd $JSR308/annotation-tools/ && ./.travis-build-without-test.sh)

## try only build checker-framework, with jdk
ant -f $JSR308/checker-framework/checker/build.xml jar

##### build checker-framework-inference
if [ -d $JSR308/checker-framework-inference ] ; then
    (cd $JSR308/checker-framework-inference && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/checker-framework-inference.git)
fi

(cd $JSR308/checker-framework-inference && gradle dist)

##### build generic-type-inference-solver
if [ -d $JSR308/generic-type-inference-solver ] ; then
    (cd $JSR308/generic-type-inference-solver && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/generic-type-inference-solver.git)
fi

(cd $JSR308/generic-type-inference-solver/ && gradle build)

##### build ontology
(cd $JSR308/ontology && gradle build)