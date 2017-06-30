package ontology.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import checkers.inference.InferenceMain;
import checkers.inference.model.Constraint;
import ontology.qual.OntologyValue;

public class OntologyStatisticUtil {

    private final static Map<OntologyValue, Integer> valueToHashMap;
    private final static Map<Integer, OntologyValue> hashToValueMap;
    private static PostVerificationStatisticRecorder recorder;

    static {
        EnumMap<OntologyValue, Integer> tempV2HMap = new EnumMap<> (OntologyValue.class);
        HashMap<Integer, OntologyValue> tempH2VMap = new HashMap<>();

        int i = 0;
        for (OntologyValue value : OntologyValue.values()) {
            tempV2HMap.put(value, Integer.valueOf(i));
            tempH2VMap.put(Integer.valueOf(i), value);
            ++ i;
        }
        valueToHashMap = Collections.unmodifiableMap(tempV2HMap);
        hashToValueMap = Collections.unmodifiableMap(tempH2VMap);
    }

    /**
     * Persistent general information of inferred slots into file {@code filename}.
     * Information include:
     * <ul>
     *      <li> total number of inferred slots
     *      <li> inferred slots number of each {@link OntologyValue}
     * </ul>
     * @param filename the name of file that will store these information
     * @param result a map from slot id to inferred annotation result
     * 
     * <p><em>Note</em>: file will be stored at the current working directory
     */
    public static void writeInferenceResult(String filename, Map<Integer, AnnotationMirror> result) {
        String writePath = new File(new File("").getAbsolutePath()).toString() + File.separator + filename;
        StringBuilder sb = new StringBuilder();

        recordKeyValue(sb, "total_number", String.valueOf(result.size()));

        // get statistic for slots number of each combination of ontology values
        Map<String, Integer> valuesStatistics = new HashMap<>();

        for (AnnotationMirror inferAnno : result.values()) {
            if (!AnnotationUtils.areSameIgnoringValues(inferAnno, OntologyUtils.ONTOLOGY)) {
                InferenceMain.getInstance().logger.warning("OntologyStatisticUtil: found non ontology solution: " + inferAnno);
                continue;
            }

            String hashcode = getHashCode(OntologyUtils.getOntologyValues(inferAnno));

            if (hashcode.isEmpty()) {
                continue;
            }

            if (!valuesStatistics.containsKey(hashcode)) {
                valuesStatistics.put(hashcode, Integer.valueOf(1));
            } else {
                valuesStatistics.put(hashcode, valuesStatistics.get(hashcode) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : valuesStatistics.entrySet()) {
            OntologyValue[] values = getOntologyValues(entry.getKey());
            StringBuilder valueNames = new StringBuilder();
            valueNames.append("[");
            for (int i = 0; i < values.length; i ++ ) {
                valueNames.append(values[i].toString());
                if (i < values.length - 1) {
                    valueNames.append(",");
                }
            }
            valueNames.append("]");
            recordKeyValue(sb, valueNames.toString(), String.valueOf(entry.getValue()));
        }

        try {
            PrintWriter pw = new PrintWriter(writePath);
            pw.write(sb.toString());
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getHashCode(OntologyValue... values) {
        if (values == null || values.length < 1 || values[0] == null) {
            return new String();
        }

        List<Integer> list = new ArrayList<> ();
        for (OntologyValue value : values) {
            list.add(valueToHashMap.get(value));
        }
        Collections.sort(list);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i ++) {
            sb.append(String.valueOf(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static OntologyValue[] getOntologyValues(String hashcode) {
        List<OntologyValue> list = new ArrayList<>();

        String[] singleHashs = hashcode.split(",");
        for (String strHash : singleHashs) {
            OntologyValue value = hashToValueMap.get(Integer.valueOf(strHash));
            list.add(value);
        }
        return list.toArray(new OntologyValue[list.size()]);
    }

    public static PostVerificationStatisticRecorder getPostVerifyStatRecorder() {
        if (recorder == null) {
            recorder = new PostVerificationStatisticRecorder();
        }
        return recorder;
    }

    private static void recordKeyValue(StringBuilder sb, String key, String value) {
        sb.append(key + "," + value + "\n");
    }
    /**
     * An inner class for recording statistic information
     * of post-verification of Ontology solutions.
     * @author charleszhuochen
     *
     */
    public static class PostVerificationStatisticRecorder {
        private Set<Constraint> missingSolutionCons;
        private Set<ViolatedConsDiagnostic> violatedConsDiags;
        /**whether store verbose informations. currently this parameter doesn't exposed to outside setting,
         * which means one may need hard reset value here if trigger verbose. Modify it if needs.*/
        private boolean verbose = false;

        public PostVerificationStatisticRecorder() {
            missingSolutionCons = new HashSet<> ();
            violatedConsDiags = new HashSet<>();
        }

        public void recordMissingSolution(Constraint cons) {
            if (!missingSolutionCons.contains(cons)) {
                missingSolutionCons.add(cons);
            }
        }

        public void recordViolatedConsDiags(ViolatedConsDiagnostic consDiag) {
            if (!violatedConsDiags.contains(consDiag)) {
                violatedConsDiags.add(consDiag);
            }
        }

        public void writeStatistic() {
            writeStatistic("post-verification-statistic.txt");
        }
        public void writeStatistic(String filename) {
            String writePath = new File(new File("").getAbsolutePath()).toString() + File.separator + filename;
            StringBuilder sb = new StringBuilder();

            // write missing constraint info
            OntologyStatisticUtil.recordKeyValue(sb, "missing_number", String.valueOf(missingSolutionCons.size()));
            if (verbose) {
                for (Constraint cons : missingSolutionCons) {
                    sb.append(cons + "\n\n");
                }
            }
            // write violated constraint info
            OntologyStatisticUtil.recordKeyValue(sb, "violated_number", String.valueOf(violatedConsDiags.size()));

            if (verbose) {
                for (ViolatedConsDiagnostic diag : violatedConsDiags) {
                    sb.append(diag + "\n\n");
                }
            }

            try {
                PrintWriter pw = new PrintWriter(writePath);
                pw.write(sb.toString());
                pw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void printToFile(String fileName, String content) {
        try {
            PrintWriter pw = new PrintWriter(fileName);
            pw.write(content);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
