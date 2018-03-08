package ontology.util;

import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
import ontology.qual.PolyOntology;

import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TypesUtils;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class OntologyUtils {

    private static OntologyUtils singletonInstance;

    public static AnnotationMirror ONTOLOGY, ONTOLOGY_TOP, ONTOLOGY_BOTTOM, POLY_ONTOLOGY;

    /**
     * Util for operating elements.
     * Obtained from {@link ProcessingEnvironment}
     */
    private final Elements elements;

    /**
     * Util for operating types.
     * Obtained from {@link ProcessingEnvironment}
     */
    private final Types types;

    /**
     * TypeMirror for java.util.List.
     */
    private final TypeMirror LIST;

    private OntologyUtils(ProcessingEnvironment processingEnv) {
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        ONTOLOGY_TOP = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.TOP);
        ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.BOTTOM);
        ONTOLOGY = AnnotationBuilder.fromClass(elements, Ontology.class);
        POLY_ONTOLOGY = AnnotationBuilder.fromClass(elements, PolyOntology.class);

        // Built-in ontic concepts for isomorphic types.
        LIST = elements.getTypeElement("java.util.List").asType();
    }

    public static void initOntologyUtils (ProcessingEnvironment processingEnv) {
        if (singletonInstance == null) {
            singletonInstance = new OntologyUtils(processingEnv);
        }
    }

    public static OntologyUtils getInstance() {
        if (singletonInstance == null) {
            ErrorReporter.errorAbort("getInstance() get called without initialization!");
        }
        return singletonInstance;
    }

    public OntologyValue determineOntologyValue(TypeMirror type) {
        if (TypesUtils.isErasedSubtype(type, LIST, types)
                || type.getKind().equals(TypeKind.ARRAY)) {
            return OntologyValue.SEQUENCE;
        }
        // cannot determine OntologyValue by the given type
        return OntologyValue.TOP;
    }

    public static boolean isOntologyTop(AnnotationMirror type) {
        if (!AnnotationUtils.areSameIgnoringValues(ONTOLOGY, type)) {
            return false;
        }

        OntologyValue[] values = getOntologyValues(type);
        for (OntologyValue value : values) {
            if (value == OntologyValue.TOP) {
                return true;
            }
        }

        return false;
    }

    public static AnnotationMirror createOntologyAnnotationByValues(ProcessingEnvironment processingEnv,
            OntologyValue... values) {
        validateOntologyValues(values);
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Ontology.class);
        builder.setValue("values", values);
        return builder.build();
    }

    public static OntologyValue[] getOntologyValues(AnnotationMirror type) {
        List<OntologyValue> ontologyValueList = AnnotationUtils.getElementValueEnumArray(type, "values", OntologyValue.class, true);
        return ontologyValueList.toArray(new OntologyValue[ontologyValueList.size()]);
    }

    public static EnumSet<OntologyValue> lubOfOntologyValues(EnumSet<OntologyValue> valueSet1, EnumSet<OntologyValue> valueSet2) {
        EnumSet<OntologyValue> lub = EnumSet.noneOf(OntologyValue.class);

        for (OntologyValue value1 : valueSet1) {
            if (value1 == OntologyValue.TOP) {
                lub.clear();
                break;
            }
            if (valueSet2.contains(value1)) {
                lub.add(value1);
            }
        }

        if (lub.isEmpty()) {
            lub.add(OntologyValue.TOP);
        }

        return lub;
    }

    /**
     * check whether the passed values are validated as arguments of Ontology qualifier
     * valid values should not be null, and contains at least one ontology value, and
     * doesn't cotains null element inside the array.
     * @param values the checking values
     */
    protected static void validateOntologyValues(OntologyValue... values) {
        if (values == null || values.length < 1) {
            ErrorReporter.errorAbort("ontology values are invalid: " + values);
        }
        for (OntologyValue value : values) {
            if (value == null) {
                ErrorReporter.errorAbort("ontology values are invalid: " + values);
            }
        }
    }
}
