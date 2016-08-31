package ontology;

import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;

import checkers.inference.model.ConstraintManager;
import checkers.inference.InferenceAnnotatedTypeFactory;
import checkers.inference.InferenceChecker;
import checkers.inference.InferenceQualifierHierarchy;
import checkers.inference.InferenceTreeAnnotator;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.VariableAnnotator;
import checkers.inference.model.ConstantSlot;
import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;

public class OntologyInferenceAnnotatedTypeFactory extends InferenceAnnotatedTypeFactory {

    protected final AnnotationMirror ONTOLOGY;
    protected final AnnotationMirror ONTOLOGY_BOTTOM;
    protected final AnnotationMirror ONTOLOGY_TOP;

    public OntologyInferenceAnnotatedTypeFactory(InferenceChecker inferenceChecker, boolean withCombineConstraints,
            BaseAnnotatedTypeFactory realTypeFactory, InferrableChecker realChecker, SlotManager slotManager,
            ConstraintManager constraintManager) {
        super(inferenceChecker, withCombineConstraints, realTypeFactory, realChecker, slotManager, constraintManager);
        ONTOLOGY = AnnotationUtils.fromClass(elements, Ontology.class);
        ONTOLOGY_BOTTOM = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.BOTTOM);
        ONTOLOGY_TOP = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.TOP);
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
        protected Set<AnnotationMirror>
        findTops(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newTops = super.findTops(supertypes);
            newTops.remove(ONTOLOGY);
            newTops.add(ONTOLOGY_TOP);
            return newTops;
        }

        @Override
        protected Set<AnnotationMirror>
        findBottoms(Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newBottoms = super.findTops(supertypes);
            newBottoms.remove(ONTOLOGY);
            newBottoms.add(ONTOLOGY_BOTTOM);
            return newBottoms;
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new ImplicitsTreeAnnotator(this),
                new OntologyInferenceTreeAnnotator(this, realChecker,
                        realTypeFactory, variableAnnotator, slotManager));
    }

    public class OntologyInferenceTreeAnnotator extends InferenceTreeAnnotator {

        private final VariableAnnotator variableAnnotator;

        public OntologyInferenceTreeAnnotator(InferenceAnnotatedTypeFactory atypeFactory,
                InferrableChecker realChecker, AnnotatedTypeFactory realAnnotatedTypeFactory,
                VariableAnnotator variableAnnotator, SlotManager slotManager) {
            super(atypeFactory, realChecker, realAnnotatedTypeFactory, variableAnnotator, slotManager);
            this.variableAnnotator = variableAnnotator;
        }

        @Override
        public Void visitNewClass(final NewClassTree newClassTree, final AnnotatedTypeMirror atm) {
            switch (OntologyUtils.determineOntologyValue(atm.getUnderlyingType())) {
            case SEQUENCE: {
                AnnotationMirror anno = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.SEQUENCE);
                ConstantSlot cs = variableAnnotator.createConstant(anno, newClassTree);
                atm.replaceAnnotation(cs.getValue());
                }
                break;

            case TOP:
            default:
                break;
            }
            variableAnnotator.visit(atm, newClassTree.getIdentifier());
            return null;
        }

        @Override
        public Void visitNewArray(final NewArrayTree newArrayTree, final AnnotatedTypeMirror atm) {
            AnnotationMirror anno = OntologyUtils.createOntologyAnnotationByValues(processingEnv, OntologyValue.SEQUENCE);
            ConstantSlot cs = variableAnnotator.createConstant(anno, newArrayTree);
            atm.replaceAnnotation(cs.getValue());
            variableAnnotator.visit(atm, newArrayTree);
            return null;
        }

        @Override
        public Void visitParameterizedType(final ParameterizedTypeTree param, final AnnotatedTypeMirror atm) {
            TreePath path = atypeFactory.getPath(param);
            if (path != null) {
                final TreePath parentPath = path.getParentPath();
                final Tree parentNode = parentPath.getLeaf();
                if (!parentNode.getKind().equals(Kind.NEW_CLASS)) {
                    variableAnnotator.visit(atm, param);
                }
            }
            return null;
        }
    }
}
