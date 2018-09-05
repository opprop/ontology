import yaml
import os, sys
import subprocess
import shlex
import argparse
import time

ONTOLOGY_DIR = os.path.dirname(os.path.realpath(__file__))

def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('--corpus-file', dest='corpus_file', required=True)
    parser.add_argument('--corpus', dest='corpus')
    parser.add_argument('--is-travis-build', type=bool, dest='is_travis_build')
    args = parser.parse_args()

    tool_excutable = os.path.join(ONTOLOGY_DIR, "run-dljc.sh")

    corpus_name = args.corpus if not args.corpus == None else os.path.splitext(args.corpus_file)[0]

    BENCHMARK_DIR = os.path.join(ONTOLOGY_DIR, corpus_name)

    print "----- Fetching corpus... -----"
    if not os.path.exists(BENCHMARK_DIR):
        print "Creating corpus dir {}.".format(BENCHMARK_DIR)
        os.makedirs(BENCHMARK_DIR)
        print "Corpus dir {} created.".format(BENCHMARK_DIR)
 
    print "Enter corpus dir {}.".format(BENCHMARK_DIR)
    os.chdir(BENCHMARK_DIR)

    projects = None
    with open (os.path.join(ONTOLOGY_DIR, args.corpus_file)) as projects_file:
        projects = yaml.load(projects_file)["projects"]

    for project_name, project_attrs in projects.iteritems():
        project_dir = os.path.join(BENCHMARK_DIR, project_name)
        if not os.path.exists(project_dir):
            git("clone", project_attrs["giturl"], "--depth", "1")

    print "----- Fetching corpus done. -----"

    print "----- Running Ontology on corpus... -----"

    successful_projects = list()
    failed_projects = list()

    for project_name, project_attrs in projects.iteritems():
        project_dir = os.path.join(BENCHMARK_DIR, project_name)
        os.chdir(project_dir)
        print "Enter directory: {}".format(project_dir)
        if project_attrs["clean"] == '' or project_attrs["build"] == '':
            print "Skip project {}, as there were no build/clean cmd.".format(project_name)
        print "Cleaning project..."
        subprocess.call(shlex.split(project_attrs["clean"]))
        print "Cleaning done."
        print "Running command: {}".format(tool_excutable + " " + project_attrs["build"])
        start = time.time()
        rtn_code = subprocess.call([tool_excutable, project_attrs["build"]])
        end = time.time()
        print "Return code is {}.".format(rtn_code)
        print "Time taken by {}: \t{}\t seconds".format(project_name, end - start)
        if not rtn_code == 0:
            failed_projects.append(project_name)
        else:
            successful_projects.append(project_name)

    if len(failed_projects) > 0:
        print "----- Inference failed on {} out of {} projects. -----".format(len(failed_projects), len(projects))
        print "  Successful projects are: {}.".format(successful_projects)
        print "  Failed projects are: {}.".format(failed_projects)
    else:
        print "----- Inference successfully inferred all {} projects. -----".format(len(projects))

    print "----- Running Ontology on corpus done. -----"

    rtn_code = 1 if len(failed_projects) > 0 else 0

    # DEBUGGING FOR TRAVIS
    if args.is_travis_build:
        print "----- Log file contents of failed projects: -----" + '\n'

        for project_name in failed_projects:
            log_file = os.path.join(BENCHMARK_DIR, project_name, "logs", "infer.log")
            print "------------------------------------------------------------"
            print log_file
            print "------------------------------------------------------------"

            try:
                log_file_content = open(log_file, "r")
                print log_file_content.read()
            except IOError:
                print "log file does not exist"

            print "------------------------------------------------------------"
            print "end of " + log_file
            print "------------------------------------------------------------"

    sys.exit(rtn_code)

def git(*args):
    return subprocess.check_call(['git'] + list(args))

if __name__ == "__main__":
    main(sys.argv)