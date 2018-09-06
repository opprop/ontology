#!/bin/bash

TESTFILE=testing/PolyOntologyTest.java
ANNOTATEDFILE=annotated/PolyOntologyTest.java

subl $TESTFILE

./run-ontology.sh $TESTFILE

subl $ANNOTATEDFILE
