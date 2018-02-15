import yaml
import os, sys
import subprocess
import shlex
import argparse

ONTOLOGY_DIR = os.path.dirname(os.path.realpath(__file__))
BENCHMARK_DIR = os.path.join(ONTOLOGY_DIR, "corpus")


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('--corpus-file', dest='corpus_file')
    args = parser.parse_args()

    tool_excutable = os.path.join(ONTOLOGY_DIR, "run-dljc.sh")

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

    print "----- Runnning Ontlogy on corpus... -----"

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
        rtn_code = subprocess.call([tool_excutable, project_attrs["build"]])
        print "Return code is {}.".format(rtn_code)
        if not rtn_code == 0:
            failed_projects.append(project_name)

    if len(failed_projects) > 0:
        print "----- Inference failed on {} out of {} projects. Failed projects are: {} -----".format(len(failed_projects), len(projects), failed_projects)
    else:
        print "----- Inference succeed infer all {} projects. -----".format(len(projects))

    print "----- Runnning Ontlogy on corpus done. -----"

    rtn_code = 1 if len(failed_projects) > 0 else 0

    sys.exit(rtn_code)

def git(*args):
    return subprocess.check_call(['git'] + list(args))

if __name__ == "__main__":
    main(sys.argv)