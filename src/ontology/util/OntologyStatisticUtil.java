package ontology.util;

import checkers.inference.InferenceMain;
import checkers.inference.InferenceResult;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.model.VariableSlot;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import ontology.qual.OntologyValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

public class OntologyStatisticUtil {

    private static final Map<OntologyValue, Integer> valueToHashMap;
    private static final Map<Integer, OntologyValue> hashToValueMap;
    private static PostVerificationStatisticRecorder recorder;

    static {
        EnumMap<OntologyValue, Integer> tempV2HMap = new EnumMap<>(OntologyValue.class);
        HashMap<Integer, OntologyValue> tempH2VMap = new HashMap<>();

        int i = 0;
        for (OntologyValue value : OntologyValue.values()) {
            tempV2HMap.put(value, Integer.valueOf(i));
            tempH2VMap.put(Integer.valueOf(i), value);
            ++i;
        }
        valueToHashMap = Collections.unmodifiableMap(tempV2HMap);
        hashToValueMap = Collections.unmodifiableMap(tempH2VMap);
    }

    /**
     * Persistent general information of inferred slots into file {@code filename}. Information
     * include:
     *
     * <ul>
     *   <li>total number of inferred slots
     *   <li>inferred slots number of each {@link OntologyValue}
     * </ul>
     *
     * @param filename the name of file that will store these information
     * @param result a map from slot id to inferred annotation result
     *     <p><em>Note</em>: file will be stored at the current working directory
     */
    public static void writeInferenceResult(
            String filename, Map<Integer, AnnotationMirror> result) {
        String writePath =
                new File(new File("").getAbsolutePath()).toString() + File.separator + filename;
        StringBuilder sb = new StringBuilder();

        recordKeyValue(sb, "total_number", String.valueOf(result.size()));

        // get statistic for slots number of each combination of ontology values
        Map<String, Integer> valuesStatistics = new HashMap<>();

        for (AnnotationMirror inferAnno : result.values()) {
            if (!AnnotationUtils.areSameByName(inferAnno, OntologyUtils.ONTOLOGY)) {
                InferenceMain.getInstance()
                        .logger
                        .warning(
                                "OntologyStatisticUtil: found non ontology solution: " + inferAnno);
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
            for (int i = 0; i < values.length; i++) {
                valueNames.append(values[i].toString());
                if (i < values.length - 1) {
                    valueNames.append(",");
                }
            }
            valueNames.append("]");
            recordKeyValue(sb, valueNames.toString(), String.valueOf(entry.getValue()));
        }

        try {
            PrintWriter pw = new PrintWriter(writePath, StandardCharsets.UTF_8.name());
            pw.write(sb.toString());
            pw.close();
        } catch (Exception e) {
            throw new BugInCF("Unexpected problem!", e);
        }
    }

    private static String getHashCode(OntologyValue... values) {
        if (values == null || values.length < 1 || values[0] == null) {
            return new String();
        }

        List<Integer> list = new ArrayList<>();
        for (OntologyValue value : values) {
            list.add(valueToHashMap.get(value));
        }
        Collections.sort(list);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            sb.append(String.valueOf(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static OntologyValue[] getOntologyValues(String hashcode) {
        List<OntologyValue> list = new ArrayList<>();

        @SuppressWarnings("StringSplitter") // TODO: investigate
        String[] singleHashs = hashcode.split(",");
        for (String strHash : singleHashs) {
            OntologyValue value = hashToValueMap.get(Integer.valueOf(strHash));
            list.add(value);
        }
        return list.toArray(new OntologyValue[list.size()]);
    }

    /**
     * verify the solution whether is consistent with constraints
     *
     * @param mergedResult the inference result
     * @param constraints the constraints
     * @param qualifierHierarchy the qualifier hierarchy
     */
    public static void verifySolution(
            InferenceResult mergedResult,
            Collection<Constraint> constraints,
            QualifierHierarchy qualifierHierarchy,
            List<Map<Integer, AnnotationMirror>> solutionMaps) {
        assert mergedResult.hasSolution() : "Should verify an inference result with a solution!";

        List<ViolatedConsDiagnostic> diagnosticList = new ArrayList<>();
        for (Constraint constraint : constraints) {
            // TODO: also verify other kinds of constraint
            // currently ontology use ExistentialConstraint and EqualityConstraint
            if (!(constraint instanceof SubtypeConstraint)) {
                continue;
            }
            SubtypeConstraint sConstraint = (SubtypeConstraint) constraint;
            Slot subtypeSlot = sConstraint.getSubtype();
            Slot supertypeSlot = sConstraint.getSupertype();
            AnnotationMirror subtype, supertype;

            // currently both suptypeSlot and supertypeSlot should be type of VariableSlot
            assert subtypeSlot instanceof VariableSlot && supertypeSlot instanceof VariableSlot;

            final int subtypeId = ((VariableSlot) subtypeSlot).getId();
            final int supertypeId = ((VariableSlot) supertypeSlot).getId();

            if (subtypeSlot instanceof ConstantSlot && supertypeSlot instanceof ConstantSlot) {
                continue;

            } else if (subtypeSlot instanceof ConstantSlot) {
                subtype = ((ConstantSlot) subtypeSlot).getValue();
                supertype = mergedResult.getSolutionForVariable(supertypeId);

                assert subtype != null;

                if (supertype == null) {
                    logNoSolution(sConstraint, subtype, supertype);
                    continue;
                }

            } else if (supertypeSlot instanceof ConstantSlot) {
                subtype = mergedResult.getSolutionForVariable(subtypeId);
                supertype = ((ConstantSlot) supertypeSlot).getValue();

                assert supertype != null;

                if (subtype == null) {
                    logNoSolution(sConstraint, subtype, supertype);
                    continue;
                }

            } else {
                subtype = mergedResult.getSolutionForVariable(subtypeId);
                supertype = mergedResult.getSolutionForVariable(supertypeId);

                if (subtype == null || supertype == null) {
                    logNoSolution(sConstraint, subtype, supertype);
                    continue;
                }
            }

            assert subtype != null && supertype != null;

            // avoid checking poly ontology
            if (AnnotationUtils.areSame(OntologyUtils.POLY_ONTOLOGY, subtype)
                    || AnnotationUtils.areSame(OntologyUtils.POLY_ONTOLOGY, supertype)) {
                continue;
            }

            if (!qualifierHierarchy.isSubtypeQualifiersOnly(subtype, supertype)) {
                ViolatedConsDiagnostic consDiagRes =
                        new ViolatedConsDiagnostic(sConstraint, subtype, supertype);

                List<AnnotationMirror> subtypeSolutions = new ArrayList<>();
                List<AnnotationMirror> supertypeSolutions = new ArrayList<>();

                for (Map<Integer, AnnotationMirror> solutionMap : solutionMaps) {
                    if (solutionMap.containsKey(subtypeId)) {
                        subtypeSolutions.add(solutionMap.get(subtypeId));
                    }
                    if (solutionMap.containsKey(supertypeId)) {
                        supertypeSolutions.add(solutionMap.get(supertypeId));
                    }
                }

                consDiagRes.setSubtypeSolutions(subtypeSolutions);
                consDiagRes.setSupertypeSolutions(supertypeSolutions);
                diagnosticList.add(consDiagRes);
                OntologyStatisticUtil.getPostVerifyStatRecorder()
                        .recordViolatedConsDiags(consDiagRes);
            }
        }

        if (!diagnosticList.isEmpty()) {
            StringBuilder sBuilder = new StringBuilder();
            sBuilder.append("solved solution doesn't consistent with below constraints: \n");
            for (ViolatedConsDiagnostic result : diagnosticList) {
                sBuilder.append(result + "\n");
            }
            throw new BugInCF(sBuilder.toString());
        }
    }

    public static void logNoSolution(
            SubtypeConstraint subtypeConstraint,
            AnnotationMirror subtype,
            AnnotationMirror supertype) {
        InferenceMain.getInstance()
                .logger
                .warning(
                        "no solution for subtype constraint: "
                                + subtypeConstraint
                                + "\tinferred subtype: "
                                + subtype
                                + "\tinferred supertype: "
                                + supertype);
        OntologyStatisticUtil.getPostVerifyStatRecorder().recordMissingSolution(subtypeConstraint);
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
     * An inner class for recording statistic information of post-verification of Ontology
     * solutions.
     *
     * @author charleszhuochen
     */
    public static class PostVerificationStatisticRecorder {
        private Set<Constraint> missingSolutionCons;
        private Set<ViolatedConsDiagnostic> violatedConsDiags;

        /**
         * whether store verbose informations. currently this parameter doesn't exposed to outside
         * setting, which means one may need hard reset value here if trigger verbose. Modify it if
         * needs.
         */
        private boolean verbose = false;

        public PostVerificationStatisticRecorder() {
            missingSolutionCons = new HashSet<>();
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
            String writePath =
                    new File(new File("").getAbsolutePath()).toString() + File.separator + filename;
            StringBuilder sb = new StringBuilder();

            // write missing constraint info
            OntologyStatisticUtil.recordKeyValue(
                    sb, "missing_number", String.valueOf(missingSolutionCons.size()));
            if (verbose) {
                for (Constraint cons : missingSolutionCons) {
                    sb.append(cons + "\n\n");
                }
            }
            // write violated constraint info
            OntologyStatisticUtil.recordKeyValue(
                    sb, "violated_number", String.valueOf(violatedConsDiags.size()));

            if (verbose) {
                for (ViolatedConsDiagnostic diag : violatedConsDiags) {
                    sb.append(diag + "\n\n");
                }
            }

            try {
                PrintWriter pw = new PrintWriter(writePath, StandardCharsets.UTF_8.name());
                pw.write(sb.toString());
                pw.close();
            } catch (Exception e) {
                throw new BugInCF("Unexpected problem!", e);
            }
        }
    }

    public static void printToFile(String fileName, String content) {
        try {
            PrintWriter pw = new PrintWriter(fileName, StandardCharsets.UTF_8.name());
            pw.write(content);
            pw.close();
        } catch (Exception e) {
            throw new BugInCF("Unexpected problem!", e);
        }
    }
}
