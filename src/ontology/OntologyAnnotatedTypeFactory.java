package ontology;

import ontology.qual.Ontology;
import ontology.qual.SpecialQualType;
import ontology.util.OntologyUtils;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;

public class OntologyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror ONTOLOGY, ONTOLOGY_BOTTOM, ONTOLOGY_TOP;
    private ExecutableElement ontologyValue = TreeUtils.getMethod("ontology.qual.Ontology", "values",
            0, processingEnv);

    public OntologyAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        ONTOLOGY_TOP = OntologyUtils.createOntologyAnnotationByValues(OntologyUtils.convert(SpecialQualType.TOP.toString()), processingEnv);
        // ONTOLOGY could simple created by AnnotationUtils, because it doesn't need to has a value
        ONTOLOGY = AnnotationUtils.fromClass(elements, Ontology.class);
        ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(OntologyUtils.convert(SpecialQualType.BOTTOM.toString()), processingEnv);
        postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new OntologyQualifierHierarchy(factory, ONTOLOGY_BOTTOM);
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defaults) {
        TypeUseLocation[] topLocations = { TypeUseLocation.ALL };
        defaults.addCheckedCodeDefaults(ONTOLOGY_TOP, topLocations);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new OntologyTreeAnnotator());
    }

    private final class OntologyQualifierHierarchy extends GraphQualifierHierarchy {

        public OntologyQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
            super(f, bottom);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, ONTOLOGY)
                    && AnnotationUtils.areSameIgnoringValues(lhs, ONTOLOGY)) {
                String[] rhsValue = getOntologyValue(rhs);
                String[] lhsValue = getOntologyValue(lhs);
                Set<String> rSet = new HashSet<String>(Arrays.asList(rhsValue));
                Set<String> lSet = new HashSet<String>(Arrays.asList(lhsValue));
                if (rSet.containsAll(lSet)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return super.isSubtype(rhs, lhs);
            }
        }

        private String[] getOntologyValue(AnnotationMirror type) {
            @SuppressWarnings("unchecked")
            List<String> allTypesList = ((List<String>) AnnotationUtils
                    .getElementValuesWithDefaults(type).get(ontologyValue).getValue());
            // types in this list is
            // org.checkerframework.framework.util.AnnotationBuilder.
            String[] allTypesInArray = new String[allTypesList.size()];
            int i = 0;
            for (Object o : allTypesList) {
                allTypesInArray[i] = o.toString();
                i++;
                // System.out.println(o.toString());
            }
            return allTypesInArray;
        }
    }

    public class OntologyTreeAnnotator extends TreeAnnotator {
        public OntologyTreeAnnotator() {
            super(OntologyAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            if (OntologyUtils.determineAnnotation(type.getUnderlyingType())) {
                AnnotationMirror ontologyValue = OntologyUtils.genereateOntologyAnnoFromNew(processingEnv);
                type.replaceAnnotation(ontologyValue);
            }
            return super.visitNewClass(node, type);
        }

        @Override
        public Void visitNewArray(final NewArrayTree newArrayTree, final AnnotatedTypeMirror atm) {
            AnnotationMirror anno = OntologyUtils.genereateOntologyAnnoFromNew(processingEnv);
            atm.replaceAnnotation(anno);
            return super.visitNewArray(newArrayTree, atm);
        }
    }
}
