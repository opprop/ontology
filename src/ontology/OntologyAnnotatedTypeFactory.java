package ontology;

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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;

public class OntologyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public OntologyAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        OntologyUtils.initOntologyUtils(processingEnv);
        postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new OntologyQualifierHierarchy(factory, OntologyUtils.ONTOLOGY_BOTTOM);
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defaults) {
        TypeUseLocation[] topLocations = {TypeUseLocation.ALL};
        defaults.addCheckedCodeDefaults(OntologyUtils.ONTOLOGY_TOP, topLocations);
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
        protected Set<AnnotationMirror>
        findBottoms(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newBottoms = super.findBottoms(supertypes);
            newBottoms.remove(OntologyUtils.ONTOLOGY);
            newBottoms.add(OntologyUtils.ONTOLOGY_BOTTOM);

            //update supertypes
            Set<AnnotationMirror> supertypesOfBtm = new HashSet<>();
            supertypesOfBtm.add(OntologyUtils.ONTOLOGY_TOP);
            supertypes.put(OntologyUtils.ONTOLOGY_BOTTOM, supertypesOfBtm);

            return newBottoms;
        }

        @Override
        protected void finish(
                QualifierHierarchy qualHierarchy,
                Map<AnnotationMirror, Set<AnnotationMirror>> fullMap,
                Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
                Set<AnnotationMirror> tops,
                Set<AnnotationMirror> bottoms,
                Object... args) {
            super.finish(qualHierarchy, fullMap, polyQualifiers, tops, bottoms, args);

            // substitue ONTOLOGY with ONTOLOGY_TOP in fullMap
            assert fullMap.containsKey(OntologyUtils.ONTOLOGY);
            Set<AnnotationMirror> ontologyTopSupers = fullMap.get(OntologyUtils.ONTOLOGY);
            fullMap.put(OntologyUtils.ONTOLOGY_TOP, ontologyTopSupers);
            fullMap.remove(OntologyUtils.ONTOLOGY);

            // update tops
            tops.remove(OntologyUtils.ONTOLOGY);
            tops.add(OntologyUtils.ONTOLOGY_TOP);
        }

        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(rhs, OntologyUtils.ONTOLOGY)
                    && AnnotationUtils.areSameIgnoringValues(lhs, OntologyUtils.ONTOLOGY)) {
                OntologyValue[] rhsValue = OntologyUtils.getOntologyValues(rhs);
                OntologyValue[] lhsValue = OntologyUtils.getOntologyValues(lhs);
                EnumSet<OntologyValue> rSet = EnumSet.noneOf(OntologyValue.class);
                rSet.addAll(Arrays.asList(rhsValue));
                EnumSet<OntologyValue> lSet = EnumSet.noneOf(OntologyValue.class);
                lSet.addAll(Arrays.asList(lhsValue));

                if (rSet.containsAll(lSet) || rSet.contains(OntologyValue.BOTTOM) || lSet.contains(OntologyValue.TOP)) {
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
            AnnotationMirror ontologyAnno =OntologyUtils.getInstance().determineOntologyAnnotation(type.getUnderlyingType());
            if (ontologyAnno != null) {
                type.replaceAnnotation(ontologyAnno);
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
