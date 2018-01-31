#!/bin/bash

# Failed the whole script if any command failed
set -e

# Running Ontology test suite
gradle test

# Pulling DLJC, if there is no DLJC.
# This is specially for adding ontology as
# a travis downstream test for CFI.
# Because CFI doesn't pull DLJC, but ontology
# needs DLJC to run the benchmark test.
#
# TODO: I don't think this is the best place
# to place this logic of pulling DLJC, as
# it actually makes this testing script
# aware of travis. However, create a seperate
# script for only pulling DLJC also seems
# an overkill. Maybe seperate below logic
# in the future if we need to do more things
# specially for travis downstream test.
SLUGOWNER=${TRAVIS_REPO_SLUG%/*}
if [[ "$SLUGOWNER" == "" ]]; then
  SLUGOWNER=opprop
fi

if [ ! -d ../do-like-javac ] ; then
    (cd .. && git clone https://github.com/${SLUGOWNER}/do-like-javac.git)
fi

# Running Ontology on working benchmarks
python run-ontology-on-corpus.py --corpus-file worked-benchmarks.yml
