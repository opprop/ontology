#!/bin/bash

#set up common environment variables for shell scripts of ontology

export JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(dirname $(readlink -f $(/usr/bin/which java)))))}

export JSR308=$(cd $(dirname "$0")/.. && pwd)