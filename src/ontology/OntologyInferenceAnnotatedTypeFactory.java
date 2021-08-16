package ontology;

import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
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
import javax.lang.model.element.AnnotationMirror;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

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
