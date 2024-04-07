package ontology;

import checkers.inference.BaseInferenceRealTypeFactory;
import com.google.common.collect.ImmutableMap;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.BugInCF;

public class OntologyAnnotatedTypeFactory extends BaseInferenceRealTypeFactory {

    public OntologyAnnotatedTypeFactory(BaseTypeChecker checker, boolean isInfer) {
        super(checker, isInfer);
        OntologyUtils.initOntologyUtils(processingEnv);
        postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy() {
        return new OntologyQualifierHierarchy();
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

    private final class OntologyQualifierHierarchy extends ElementQualifierHierarchy {

        private final QualifierKind ONTOLOGY_KIND;

        public OntologyQualifierHierarchy() {
            super(
                    OntologyAnnotatedTypeFactory.this.getSupportedTypeQualifiers(),
                    OntologyAnnotatedTypeFactory.this.elements,
                    OntologyAnnotatedTypeFactory.this);
            this.ONTOLOGY_KIND = getQualifierKind(OntologyUtils.ONTOLOGY);
        }

        @Override
        public boolean isSubtypeQualifiers(
                AnnotationMirror subQualifier, AnnotationMirror superQualifier) {
            if (getQualifierKind(subQualifier) != ONTOLOGY_KIND
                    || getQualifierKind(superQualifier) != ONTOLOGY_KIND) {
                throw new BugInCF(
                        "unexpected annotation mirrors: %s, %s", subQualifier, superQualifier);
            }

            Set<OntologyValue> subValues = OntologyUtils.getOntologyValuesSet(subQualifier);
            Set<OntologyValue> superValues = OntologyUtils.getOntologyValuesSet(superQualifier);

            if (subValues.contains(OntologyValue.BOTTOM)
                    || superValues.contains(OntologyValue.TOP)) {
                return true;
            } else if (subValues.contains(OntologyValue.POLY)
                    && superValues.contains(OntologyValue.POLY)) {
                return true;
            } else if (subValues.contains(OntologyValue.POLY)
                    || superValues.contains(OntologyValue.POLY)) {
                return false;
            } else {
                return subValues.containsAll(superValues);
            }
        }

        @Override
        public @Nullable AnnotationMirror leastUpperBoundQualifiers(
                AnnotationMirror a1, AnnotationMirror a2) {
            if (getQualifierKind(a1) != ONTOLOGY_KIND || getQualifierKind(a2) != ONTOLOGY_KIND) {
                throw new BugInCF("unexpected annotation mirrors: %s, %s", a1, a2);
            }

            EnumSet<OntologyValue> a1Set = OntologyUtils.getOntologyValuesSet(a1);
            EnumSet<OntologyValue> a2Set = OntologyUtils.getOntologyValuesSet(a2);

            return OntologyUtils.createOntologyAnnotationByValues(
                    processingEnv,
                    OntologyUtils.lubOfOntologyValues(a1Set, a2Set)
                            .toArray(new OntologyValue[] {}));
        }

        @Override
        public @Nullable AnnotationMirror greatestLowerBoundQualifiers(
                AnnotationMirror a1, AnnotationMirror a2) {
            if (getQualifierKind(a1) != ONTOLOGY_KIND || getQualifierKind(a2) != ONTOLOGY_KIND) {
                throw new BugInCF("unexpected annotation mirrors: %s, %s", a1, a2);
            }

            EnumSet<OntologyValue> a1Set = OntologyUtils.getOntologyValuesSet(a1);
            EnumSet<OntologyValue> a2Set = OntologyUtils.getOntologyValuesSet(a2);

            return OntologyUtils.createOntologyAnnotationByValues(
                    processingEnv,
                    OntologyUtils.glbOfOntologyValues(a1Set, a2Set)
                            .toArray(new OntologyValue[] {}));
        }

        @Override
        protected Map<QualifierKind, AnnotationMirror> createTopsMap() {
            return ImmutableMap.of(
                    getQualifierKind(OntologyUtils.ONTOLOGY), OntologyUtils.ONTOLOGY_TOP);
        }

        @Override
        protected Map<QualifierKind, AnnotationMirror> createBottomsMap() {
            return ImmutableMap.of(
                    getQualifierKind(OntologyUtils.ONTOLOGY), OntologyUtils.ONTOLOGY_BOTTOM);
        }

        @Override
        public @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror start) {
            if (getQualifierKind(start) != ONTOLOGY_KIND) {
                return null;
            }
            return OntologyUtils.POLY_ONTOLOGY;
        }

        @Override
        public boolean isPolymorphicQualifier(AnnotationMirror qualifier) {
            if (getQualifierKind(qualifier) != ONTOLOGY_KIND) {
                throw new BugInCF("unexpected annotation mirror %s", qualifier);
            }

            return OntologyUtils.getOntologyValuesSet(qualifier).contains(OntologyValue.POLY);
        }
    }

    public class OntologyTreeAnnotator extends TreeAnnotator {
        public OntologyTreeAnnotator() {
            super(OntologyAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
            AnnotationMirror ontologyAnno =
                    OntologyUtils.getInstance()
                            .determineOntologyAnnotation(type.getUnderlyingType());
            if (ontologyAnno != null) {
                type.replaceAnnotation(ontologyAnno);
            }

            return super.visitNewClass(node, type);
        }

        @Override
        public Void visitNewArray(final NewArrayTree newArrayTree, final AnnotatedTypeMirror atm) {
            AnnotationMirror anno =
                    OntologyUtils.createOntologyAnnotationByValues(
                            processingEnv, OntologyValue.SEQUENCE);
            atm.replaceAnnotation(anno);
            return super.visitNewArray(newArrayTree, atm);
        }
    }
}
