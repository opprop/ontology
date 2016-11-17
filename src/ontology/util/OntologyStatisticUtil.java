package ontology.util;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;

import checkers.inference.InferenceMain;
import ontology.qual.OntologyValue;

public class OntologyStatisticUtil {

    private final static Map<OntologyValue, Integer> valueToHashMap;
    private final static Map<Integer, OntologyValue> hashToValueMap;

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

        sb.append("total inferred slots number:\t" + result.size() + "\n");

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

        sb.append("===== ontology values inferred result =====\n");

        for (Map.Entry<String, Integer> entry : valuesStatistics.entrySet()) {
            OntologyValue[] values = getOntologyValues(entry.getKey());
            for (int i = 0; i < values.length; i ++ ) {
                sb.append(values[i].toString());
                if (i < values.length - 1) {
                    sb.append(", ");
                }
            }

            sb.append("\t" + entry.getValue() + "\n");
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
}
