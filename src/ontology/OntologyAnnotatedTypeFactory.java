package ontology;

import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
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
import java.util.EnumSet;
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
        ONTOLOGY_TOP = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.TOP);
        // ONTOLOGY could simple created by AnnotationUtils, because it doesn't need to has a value
        ONTOLOGY = AnnotationUtils.fromClass(elements, Ontology.class);
        ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.BOTTOM);
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

//        @Override
//        protected Set<AnnotationMirror>
//        findTops(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
//            Set<AnnotationMirror> newTops = new HashSet<> ();
//            newTops.add(ONTOLOGY_TOP);
//            return newTops;
//        }

        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, ONTOLOGY)
                    && AnnotationUtils.areSameIgnoringValues(lhs, ONTOLOGY)) {
                OntologyValue[] rhsValue = OntologyUtils.getOntologyValues(rhs);
                OntologyValue[] lhsValue = OntologyUtils.getOntologyValues(lhs);
                EnumSet<OntologyValue> rSet = EnumSet.noneOf(OntologyValue.class);
                rSet.addAll(Arrays.asList(rhsValue));
                EnumSet<OntologyValue> lSet = EnumSet.noneOf(OntologyValue.class);
                lSet.addAll(Arrays.asList(lhsValue));

                if (rSet.containsAll(lSet)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return super.isSubtype(rhs, lhs);
            }
        }
    }

    public class OntologyTreeAnnotator extends TreeAnnotator {
        public OntologyTreeAnnotator() {
            super(OntologyAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            switch (OntologyUtils.determineOntologyValue(type.getUnderlyingType())) {
            case SEQUENCE: {
                AnnotationMirror ontologyValue = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.SEQUENCE);
                type.replaceAnnotation(ontologyValue);
                }
                break;

            case TOP:
            default:
                break;
            }
            return super.visitNewClass(node, type);
        }

        @Override
        public Void visitNewArray(final NewArrayTree newArrayTree, final AnnotatedTypeMirror atm) {
            AnnotationMirror anno = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.SEQUENCE);
            atm.replaceAnnotation(anno);
            return super.visitNewArray(newArrayTree, atm);
        }
    }
}
