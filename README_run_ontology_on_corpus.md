# Build the tool

For building the entire tool chain for Ontology, just run below command in directory `Ontology`:

```bash
# cd ontology
./pascali-setup.sh
```

# Running Ontology on sci-corpus

## Dependencies & prerequisites 

To run driver script `run-ontology-on-corpus.py`, you need first to install below packages:

- pyYaml: `pip install pyyaml`

The driver script need python version higher than `2.7.10`.

### Regarding DLJC

We currently use a customized version of [do-like-javac](https://github.com/pascaliUWat/do-like-javac.git). In `pascali-setup.sh` we download this version of DLJC in the parent dir of ontology.


## Running the tool
To run ontology on sci-corpus, just run following command:

```bash
# cd ontology
python run-ontology-on-corpus.py
```

This script would first fetching the projects defined in `projects.yml` from [opprop-benchmarks](https://github.com/opprop-benchmarks) into `ontology/corpus`, then running Ontology on this corpus.

After this script finished, you can check the `logs/infer.log` under each project directory to see the tool running log, and currently `infer_result_*.jaif` under each project directory is the infer output of onology on each project.

### Regarding the sci-corpus
You may notice currently there were some projects has been commented out in `projects.yml`, i.e. we are running on a sub-set of sci-corpus. Most of those projects cannot compile (have basic java compile issues), with two of them we still have problems to infer results on them, and currently are working on fixing them.
