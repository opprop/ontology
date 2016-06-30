package ontology.solvers.classic;

import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;
import util.PrintUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceSolution;

public class OntologySolution implements InferenceSolution {
    private final Map<Integer, EnumSet<OntologyValue>> results;
    private final Map<Integer, Boolean> idToExistance;
    private final Map<Integer, AnnotationMirror> annotationResults;

    public OntologySolution(Collection<SequenceSolution> solutions, ProcessingEnvironment processingEnv) {
        this.results = new HashMap<>();
        this.idToExistance = new HashMap<>();
        this.annotationResults = new HashMap<>();

        merge(solutions);
        createAnnotations(processingEnv);

        PrintUtils.printResult(annotationResults);
    }

    public void merge(Collection<SequenceSolution> solutions) {
        for (SequenceSolution solution : solutions) {
            mergeResults(solution);
            mergeIdToExistance(solution);
        }
    }

    private void mergeResults(SequenceSolution solution) {
        for (Map.Entry<Integer, Boolean> entry : solution.getResult().entrySet()) {
            boolean shouldContainValue = shouldContainValue(entry);
            OntologyValue value = solution.getValue();
            // TODO: what is the meaning of this if-statement?
//            if (!value.equals("")) {
              if (!value.equals(OntologyValue.TOP)) {
                EnumSet<OntologyValue> values = results.get(entry.getKey());
                if (values == null) {
                    values = EnumSet.noneOf(OntologyValue.class);
                    results.put(entry.getKey(), values);
                }

                if (shouldContainValue) {
                    values.add(value);
                }
            }
        }
    }

    protected boolean shouldContainValue(Map.Entry<Integer, Boolean> entry) {
        return entry.getValue();
    }

    private void createAnnotations(ProcessingEnvironment processingEnv) {
        for (Map.Entry<Integer, EnumSet<OntologyValue>> entry : results.entrySet()) {
            int slotId = entry.getKey();
            EnumSet<OntologyValue> values = entry.getValue();
            // TODO: this is a workaround of avoid giving empty ontology annotation solutions,
            // should investigate why empty solution gennerated
            if (values == null || values.size() == 0) {
                continue;
            }

            AnnotationMirror anno = OntologyUtils.createOntologyAnnotationByValues(processingEnv,
                    values.toArray(new OntologyValue[values.size()]));
            annotationResults.put(slotId, anno);
        }
    }

    private void mergeIdToExistance(SequenceSolution solution) {
        for (Map.Entry<Integer, Boolean> entry : solution.getResult().entrySet()) {
            int id = entry.getKey();
            boolean existsValue = entry.getValue();
            if (idToExistance.containsKey(id)) {
                boolean alreadyExists = idToExistance.get(id);
                if (alreadyExists ^ existsValue) {
                    InferenceMain.getInstance().logger.log(Level.INFO, "Mismatch between existance of annotation");
                }
            } else {
                idToExistance.put(id, existsValue);
            }
        }
    }

    @Override
    public boolean doesVariableExist(int varId) {
        return idToExistance.containsKey(varId);
    }

    @Override
    public AnnotationMirror getAnnotation(int varId) {
        return annotationResults.get(varId);
    }

}
