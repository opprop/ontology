package ontology;

import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic;
import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * This class checks the well-formedness of ontology values used inside an {@link Ontology}
 * annotation. The current rules include: 1. If the type is TOP, BOTTOM, or POLY, the annotation
 * shouldn't contain other values (e.g., @Ontology({POLY, FORCE_3D}) is invalid).
 */
public class OntologyTypeValidator extends InferenceValidator {
    public OntologyTypeValidator(
            BaseTypeChecker checker,
            InferenceVisitor<?, ?> visitor,
            AnnotatedTypeFactory atypeFactory) {
        super(checker, visitor, atypeFactory);
    }

    @Override
    protected List<DiagMessage> isTopLevelValidType(AnnotatedTypeMirror type) {
        List<DiagMessage> errorMsgs = new ArrayList<>(super.isTopLevelValidType(type));

        AnnotationMirror am = type.getAnnotation(Ontology.class);
        if (am != null) {
            Set<OntologyValue> values = OntologyUtils.getOntologyValuesSet(am);
            if (values.size() > 1) {
                if (values.contains(OntologyValue.POLY)
                        || values.contains(OntologyValue.TOP)
                        || values.contains(OntologyValue.BOTTOM)) {
                    // Should only have one ontology value when the type is one of {POLY, TOP,
                    // BOTTOM}
                    errorMsgs.add(
                            new DiagMessage(
                                    Diagnostic.Kind.ERROR,
                                    "type.invalid.conflicting.elements",
                                    am,
                                    type));
                }
            }
        }

        return errorMsgs;
    }
}
