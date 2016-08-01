package ontology.util;

import ontology.qual.Ontology;
import ontology.qual.OntologyValue;

import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.TypesUtils;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class OntologyUtils {

    public static OntologyValue determineOntologyValue(TypeMirror type) {
        if (TypesUtils.isDeclaredOfName(type, "java.util.LinkedList")
                || TypesUtils.isDeclaredOfName(type, "java.util.ArrayList")
                || type.getKind().equals(TypeKind.ARRAY)) {
            return OntologyValue.SEQUENCE;
        }
        // cannot determine OntologyValue by the given type
        return OntologyValue.TOP;
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
