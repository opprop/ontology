#!/bin/bash
WORKING_DIR=$(cd $(dirname "$0") && pwd)

#setup env variables
. ./env-setup.sh

export REPO_SITE=opprop 

### building checker framework stuffs and ontology
. ./pascali-setup.sh


### fetching annotated projects

PROJECTS_DIR=$JSR308/annotatedProjects
PROJECTS_DATA=$JSR308/ontology/projects.data

##
#$1: project_info, format is "project_gitUrl branch" or "project_url" which the master branch will be apply
#do a light clone (depth 1) of a given git project to current directory
function downloadGitProject() {

    project_url=$1

    if [ $# == "2" ]; then
        branch=$2;
    else
        branch="master"
    fi

    git clone $project_url --branch $branch --depth=1
} 

function runOntologyOnProject() {
    mvn="pom.xml"
    gradle="build.gradle"
    ant="build.xml"
    # echo "project: $1"
    cd $1
    #determine build cmd
    if [ -e $ant ]; then
        build_cmd="ant"
    elif [ -e $gradle ]; then
        build_cmd="gradle"
    elif [ -e $mvn ]; then
        build_cmd="mvn install"
    else
        echo "don't know for $1"
        return 1
    fi

    echo "$1 $build_cmd"
    $JSR308/ontology/run-dljc.sh $build_cmd | tee ontology.log
}

# remove legacy projects
if [ -d "$PROJECTS_DIR" ]; then
    rm -rf "$PROJECTS_DIR"
fi

mkdir "$PROJECTS_DIR"

cd $PROJECTS_DIR

while read line
do
    downloadGitProject $line
done < $PROJECTS_DATA

for project in $PROJECTS_DIR/*
do
    runOntologyOnProject $project
done

### collecting statistics

