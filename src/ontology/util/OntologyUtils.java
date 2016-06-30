package ontology.util;

import ontology.qual.Ontology;
import ontology.qual.OntologyValue;

import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class OntologyUtils {

    public static boolean determineAnnotation(TypeMirror type) {
        if (TypesUtils.isDeclaredOfName(type, "java.util.LinkedList")
                || TypesUtils.isDeclaredOfName(type, "java.util.ArrayList")
                || type.getKind().equals(TypeKind.ARRAY)) {
            return true;
        }
        return false;
    }

    public static AnnotationMirror createOntologyAnnotationByValues(ProcessingEnvironment processingEnv,
            OntologyValue... values) {
        // for varargs the non-null assertion becomes a little tricky:
        // http://stackoverflow.com/questions/11919076/varargs-and-null-argument
        assert values != null : "null values unexpected";
        assert values.length > 0 : "zero size values unexpected";
        assert values[0] != null : "values array of null unexpected";

        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Ontology.class);
        builder.setValue("values", values);
        return builder.build();
    }

    public static OntologyValue[] getOntologyValues(AnnotationMirror type) {
        List<OntologyValue> ontologyValueList = AnnotationUtils.getElementValueEnumArray(type, "values", OntologyValue.class, true);
        return ontologyValueList.toArray(new OntologyValue[ontologyValueList.size()]);
    }

}
