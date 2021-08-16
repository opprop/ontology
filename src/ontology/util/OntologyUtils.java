package ontology.util;

import java.util.EnumSet;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
import ontology.qual.PolyOntology;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class OntologyUtils {

    private static OntologyUtils singletonInstance;

    public static AnnotationMirror ONTOLOGY, ONTOLOGY_TOP, ONTOLOGY_BOTTOM, POLY_ONTOLOGY;

    private static ExecutableElement ontologyValueElement;

    /** The processing environment. */
    private final ProcessingEnvironment processingEnvironment;

    /** Util for operating elements. Obtained from {@link ProcessingEnvironment} */
    private final Elements elements;

    /** Util for operating types. Obtained from {@link ProcessingEnvironment} */
    private final Types types;

    /** TypeMirror for java.util.List. */
    private final TypeMirror LIST;

    /** TypeMirror for java.util.Dictionary; */
    private final TypeMirror DICTIONARY;

    /** TypeMirror for java.util.Map; */
    private final TypeMirror MAP;

    private OntologyUtils(ProcessingEnvironment processingEnv) {
        processingEnvironment = processingEnv;
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        ONTOLOGY_TOP =
                OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.TOP);
        ONTOLOGY_BOTTOM =
                OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.BOTTOM);
        ONTOLOGY = AnnotationBuilder.fromClass(elements, Ontology.class);
        POLY_ONTOLOGY = AnnotationBuilder.fromClass(elements, PolyOntology.class);

        ontologyValueElement = TreeUtils.getMethod(Ontology.class, "values", processingEnv);

        // Built-in ontic concepts for isomorphic types.
        LIST = elements.getTypeElement("java.util.List").asType();
        DICTIONARY = elements.getTypeElement("java.util.Dictionary").asType();
        MAP = elements.getTypeElement("java.util.Map").asType();
    }

    public static void initOntologyUtils(ProcessingEnvironment processingEnv) {
        if (singletonInstance == null) {
            singletonInstance = new OntologyUtils(processingEnv);
        }
    }

    public static OntologyUtils getInstance() {
        if (singletonInstance == null) {
            throw new BugInCF("getInstance() get called without initialization!");
        }
        return singletonInstance;
    }

    public AnnotationMirror determineOntologyAnnotation(TypeMirror type) {
        OntologyValue determinedValue = OntologyValue.TOP;

        if (TypesUtils.isErasedSubtype(type, LIST, types)
                || type.getKind().equals(TypeKind.ARRAY)) {
            determinedValue = OntologyValue.SEQUENCE;
        }
        if (TypesUtils.isErasedSubtype(type, MAP, types)
                || TypesUtils.isErasedSubtype(type, DICTIONARY, types)) {
            determinedValue = OntologyValue.DICTIONARY;
        }

        switch (determinedValue) {
            case TOP:
            case BOTTOM:
                return null;
            default:
                {
                    return createOntologyAnnotationByValues(processingEnvironment, determinedValue);
                }
        }
    }

    public static boolean isOntologyTop(AnnotationMirror type) {
        if (!AnnotationUtils.areSameByName(ONTOLOGY, type)) {
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

    public static AnnotationMirror createOntologyAnnotationByValues(
            ProcessingEnvironment processingEnv, OntologyValue... values) {
        validateOntologyValues(values);
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Ontology.class);
        builder.setValue("values", values);
        return builder.build();
    }

    public static OntologyValue[] getOntologyValues(AnnotationMirror type) {
        return AnnotationUtils.getElementValueEnumArray(
                type,
                ontologyValueElement,
                OntologyValue.class,
                new OntologyValue[] {OntologyValue.TOP});
    }

    public static EnumSet<OntologyValue> lubOfOntologyValues(
            EnumSet<OntologyValue> valueSet1, EnumSet<OntologyValue> valueSet2) {
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
     * check whether the passed values are validated as arguments of Ontology qualifier valid values
     * should not be null, and contains at least one ontology value, and doesn't cotains null
     * element inside the array.
     *
     * @param values the checking values
     */
    protected static void validateOntologyValues(OntologyValue... values) {
        if (values == null || values.length < 1) {
            throw new BugInCF("ontology values are invalid: " + values);
        }
        for (OntologyValue value : values) {
            if (value == null) {
                throw new BugInCF("ontology values are invalid: " + values);
            }
        }
    }
}
