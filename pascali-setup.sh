#!/bin/bash

# Fail the whole script if any command fails
set -e

WORKING_DIR=$(cd $(dirname "$0") && pwd)
. $WORKING_DIR/env-setup.sh

# export SHELLOPTS

#default value is pascaliUWat. REPO_SITE may be set to other value for travis test purpose.
export REPO_SITE="${REPO_SITE:-pascaliUWat}"

echo "------ Downloading everthing from REPO_SITE: $REPO_SITE ------"

##### Fetching checker-framework
if [ -d $JSR308/checker-framework ] ; then
    (cd $JSR308/checker-framework && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/checker-framework.git)
fi

## Fetching annotation-tools (Annotation File Utilities)
if [ -d $JSR308/annotation-tools ] ; then
    # Older versions of git don't support the -C command-line option
    (cd $JSR308/annotation-tools && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/typetools/annotation-tools.git)
fi

# Build annotation tools, this also builds jsr308-langtools
(cd $JSR308/annotation-tools/ && ./.travis-build-without-test.sh)

## Fetching stubparser
if [ -d $JSR308/stubparser ] ; then
    (cd $JSR308/stubparser && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/typetools/stubparser.git)
fi

## Fetching DLJC
if [ -d $JSR308/do-like-javac ] ; then
    (cd $JSR308/do-like-javac && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/do-like-javac.git)
fi

## Fast-build stubparser without testing.
(cd $JSR308/stubparser && mvn -Dmaven.test.skip=true install)

## Try only build checker-framework, with jdk
(ant -f $JSR308/checker-framework/checker/build.xml jar)

##### build checker-framework-inference
if [ -d $JSR308/checker-framework-inference ] ; then
    (cd $JSR308/checker-framework-inference && git pull)
else
    (cd $JSR308 && git clone --depth 1 https://github.com/"$REPO_SITE"/checker-framework-inference.git)
fi

(cd $JSR308/checker-framework-inference && gradle dist)

##### build ontology without testing
(cd $JSR308/ontology && gradle build -x test)
