import yaml
import os, sys
import subprocess
import shlex

ONTOLOGY_DIR = os.path.dirname(os.path.realpath(__file__))
BENCHMARK_DIR = os.path.join(ONTOLOGY_DIR, "corpus")


TOOLS = {
    "ontology" : "run-dljc.sh",
}

def main(argv):
    tool_name = "ontology"
    tool_excutable = os.path.join(ONTOLOGY_DIR, TOOLS[tool_name])

    print "----- Fetching corpus... -----"
    if not os.path.exists(BENCHMARK_DIR):
        print "Creating corpus dir {}.".format(BENCHMARK_DIR)
        os.makedirs(BENCHMARK_DIR)
        print "Corpus dir {} created.".format(BENCHMARK_DIR)
 
    print "Enter corpus dir {}.".format(BENCHMARK_DIR)
    os.chdir(BENCHMARK_DIR)

    with open (os.path.join(ONTOLOGY_DIR, "projects.yml")) as projects:
        for project in yaml.load(projects)["projects"]:
            project_dir = os.path.join(BENCHMARK_DIR, project["name"])
            if not os.path.exists(project_dir):
                git("clone", project["giturl"], "--depth", "1")

    print "----- Fetching corpus done. -----"

    print "----- Runnning Ontlogy on corpus... -----"

    with open (os.path.join(ONTOLOGY_DIR, "projects.yml")) as projects:
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

    print "----- Runnning Ontlogy on corpus done. -----"

def git(*args):
    return subprocess.check_call(['git'] + list(args))

if __name__ == "__main__":
    main(sys.argv)