#!/bin/bash

# Failed the whole script if any command failed
set -e

# Running Ontology test suite
gradle test

# Running Ontology on working benchmarks
python run-ontology-on-corpus.py --corpus-file worked-benchmarks.yml
