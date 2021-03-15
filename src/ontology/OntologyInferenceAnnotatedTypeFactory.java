package ontology;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceQualifierHierarchy;
import checkers.inference.InferenceTreeAnnotator;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.ConstraintManager;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;

public class OntologyInferenceAnnotatedTypeFactory extends InferenceAnnotatedTypeFactory {

    public OntologyInferenceAnnotatedTypeFactory(
            InferenceChecker inferenceChecker,
            boolean withCombineConstraints,
            BaseAnnotatedTypeFactory realTypeFactory,
            InferrableChecker realChecker,
            SlotManager slotManager,
            ConstraintManager constraintManager) {
        super(
                inferenceChecker,
                withCombineConstraints,
                realTypeFactory,
                realChecker,
                slotManager,
                constraintManager);
        OntologyUtils.initOntologyUtils(processingEnv);
        postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new OntologyInferenceQualifierHierarchy(factory);
    }

    public class OntologyInferenceQualifierHierarchy extends InferenceQualifierHierarchy {

        public OntologyInferenceQualifierHierarchy(MultiGraphFactory multiGraphFactory) {
            super(multiGraphFactory);
        }

        @Override
        protected Set<AnnotationMirror> findBottoms(
                Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newBottoms = super.findBottoms(supertypes);
            newBottoms.remove(OntologyUtils.ONTOLOGY);
            newBottoms.add(OntologyUtils.ONTOLOGY_BOTTOM);

            // update supertypes
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
        }
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defaults) {
        TypeUseLocation[] topLocations = {TypeUseLocation.ALL};
        defaults.addCheckedCodeDefaults(OntologyUtils.ONTOLOGY_TOP, topLocations);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new LiteralTreeAnnotator(this),
                new OntologyInferenceTreeAnnotator(
                        this, realChecker, realTypeFactory, variableAnnotator, slotManager));
    }

    public class OntologyInferenceTreeAnnotator extends InferenceTreeAnnotator {

        private final VariableAnnotator variableAnnotator;

        public OntologyInferenceTreeAnnotator(
                InferenceAnnotatedTypeFactory atypeFactory,
                InferrableChecker realChecker,
                AnnotatedTypeFactory realAnnotatedTypeFactory,
                VariableAnnotator variableAnnotator,
                SlotManager slotManager) {
            super(
                    atypeFactory,
                    realChecker,
                    realAnnotatedTypeFactory,
                    variableAnnotator,
                    slotManager);
            this.variableAnnotator = variableAnnotator;
        }

        @Override
        public Void visitNewClass(final NewClassTree newClassTree, final AnnotatedTypeMirror atm) {
            AnnotationMirror ontologyAnno =
                    OntologyUtils.getInstance()
                            .determineOntologyAnnotation(atm.getUnderlyingType());
            if (ontologyAnno != null) {
                ConstantSlot cs = variableAnnotator.createConstant(ontologyAnno, newClassTree);
                atm.replaceAnnotation(cs.getValue());
            }

            variableAnnotator.visit(atm, newClassTree.getIdentifier());
            return null;
        }

        @Override
        public Void visitNewArray(final NewArrayTree newArrayTree, final AnnotatedTypeMirror atm) {
            AnnotationMirror anno =
                    OntologyUtils.createOntologyAnnotationByValues(
                            processingEnv, OntologyValue.SEQUENCE);
            ConstantSlot cs = variableAnnotator.createConstant(anno, newArrayTree);
            atm.replaceAnnotation(cs.getValue());
            variableAnnotator.visit(atm, newArrayTree);
            return null;
        }

        @Override
        public Void visitParameterizedType(
                final ParameterizedTypeTree param, final AnnotatedTypeMirror atm) {
            TreePath path = atypeFactory.getPath(param);
            if (path == null || !path.getParentPath().getLeaf().getKind().equals(Kind.NEW_CLASS)) {
                variableAnnotator.visit(atm, param);
            }

            return null;
        }
    }
}
