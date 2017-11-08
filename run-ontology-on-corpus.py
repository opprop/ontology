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

    with open (os.path.join(ONTOLOGY_DIR, args.corpus_file)) as projects:
        for project in yaml.load(projects)["projects"]:
            project_dir = os.path.join(BENCHMARK_DIR, project["name"])
            if not os.path.exists(project_dir):
                git("clone", project["giturl"], "--depth", "1")

    print "----- Fetching corpus done. -----"

    print "----- Runnning Ontlogy on corpus... -----"

    failed_projects = list()
    with open (os.path.join(ONTOLOGY_DIR, args.corpus_file)) as projects:
        for project in yaml.load(projects)["projects"]:
            project_dir = os.path.join(BENCHMARK_DIR, project["name"])
            project_dir = os.path.join(BENCHMARK_DIR, project["name"])
            os.chdir(project_dir)
            print "Enter directory: {}".format(project_dir)
            if project["clean"] == '' or project["build"] == '':
                print "Skip project {}, as there were no build/clean cmd.".format(project["name"])
            print "Cleaning project..."
            subprocess.call(shlex.split(project["clean"]))
            print "Cleaning done."
            print "Running command: {}".format(tool_excutable + " " + project["build"])
            rtn_code = subprocess.call([tool_excutable, project["build"]])
            print "Return code is {}.".format(rtn_code)
            if not rtn_code == 0:
                failed_projects.append(project["name"])

    if len(failed_projects) > 0:
        print "----- Inference failed on projects: {} -----".format(failed_projects)
    else:
        print "----- Inference succeed infer all projects. -----"

    print "----- Runnning Ontlogy on corpus done. -----"

    rtn_code = 1 if len(failed_projects) > 0 else 0

    sys.exit(rtn_code)

def git(*args):
    return subprocess.check_call(['git'] + list(args))

if __name__ == "__main__":
    main(sys.argv)