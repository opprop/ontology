package ontology;

import checkers.inference.InferenceChecker;
import checkers.inference.InferenceValidator;
import checkers.inference.InferenceVisitor;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;

public class OntologyVisitor extends InferenceVisitor<OntologyChecker, BaseAnnotatedTypeFactory> {

    public OntologyVisitor(
            OntologyChecker checker,
            InferenceChecker ichecker,
            BaseAnnotatedTypeFactory factory,
            boolean infer) {
        super(checker, ichecker, factory, infer);
    }

    @Override
    protected InferenceValidator createTypeValidator() {
        return new OntologyTypeValidator(checker, this, atypeFactory);
    }
}
