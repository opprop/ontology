package ontology;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFTransfer;

import checkers.inference.BaseInferrableChecker;
//import checkers.inference.ConstraintManager;
import checkers.inference.model.ConstraintManager;
import checkers.inference.InferenceChecker;
import checkers.inference.InferrableChecker;
import checkers.inference.SlotManager;
import checkers.inference.dataflow.InferenceAnalysis;
import checkers.inference.dataflow.InferenceTransfer;

public class OntologyChecker extends BaseInferrableChecker {

    @Override
    public void initChecker() {
        super.initChecker();
    }

    @Override
    public OntologyVisitor createVisitor(InferenceChecker ichecker, BaseAnnotatedTypeFactory factory, boolean infer) {
        return new OntologyVisitor(this, ichecker, factory, infer);
    }

    @Override
    public OntologyAnnotatedTypeFactory createRealTypeFactory() {
        return new OntologyAnnotatedTypeFactory(this);
    }

    @Override
    public CFTransfer createInferenceTransferFunction(InferenceAnalysis analysis) {
        return new InferenceTransfer(analysis);
    }

    @Override
    public OntologyInferenceAnnotatedTypeFactory createInferenceATF(InferenceChecker inferenceChecker,
            InferrableChecker realChecker, BaseAnnotatedTypeFactory realTypeFactory,
            SlotManager slotManager, ConstraintManager constraintManager) {
        OntologyInferenceAnnotatedTypeFactory ontologyInferenceATF = new OntologyInferenceAnnotatedTypeFactory(
                inferenceChecker, realChecker.withCombineConstraints(), realTypeFactory, realChecker,
                slotManager, constraintManager);
        return ontologyInferenceATF;
    }

    @Override
    public boolean isInsertMainModOfLocalVar() {
        return true;
    }

    @Override
    public boolean withCombineConstraints() {
        return false;
    }
}
