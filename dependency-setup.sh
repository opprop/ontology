#!/bin/bash

# Fail the whole script if any command fails
set -e

WORKING_DIR=$(cd $(dirname "$0") && pwd)
. $WORKING_DIR/env-setup.sh

# export SHELLOPTS

if [ -d "/tmp/plume-scripts" ] ; then
  git -C /tmp/plume-scripts pull -q
else
  git -C /tmp clone --depth 1 -q https://github.com/plume-lib/plume-scripts.git
fi

#default value is opprop. REPO_SITE may be set to other value for travis test purpose.
export REPO_SITE="${REPO_SITE:-opprop}"

echo "------ Downloading everything from REPO_SITE: $REPO_SITE ------"

##### build checker-framework-inference
if [ -d $JSR308/checker-framework-inference ] ; then
    (cd $JSR308/checker-framework-inference && git pull)
else
    /tmp/plume-scripts/git-clone-related $REPO_SITE checker-framework-inference $JSR308/checker-framework-inference
fi

(cd $JSR308/checker-framework-inference && ./.ci-build-without-test.sh)

## Fetching DLJC
if [ -d $JSR308/do-like-javac ] ; then
    (cd $JSR308/do-like-javac && git pull)
else
    /tmp/plume-scripts/git-clone-related $REPO_SITE do-like-javac $JSR308/do-like-javac
fi

##### build ontology without testing
(cd $JSR308/ontology && ./gradlew build -x test --console=plain)
