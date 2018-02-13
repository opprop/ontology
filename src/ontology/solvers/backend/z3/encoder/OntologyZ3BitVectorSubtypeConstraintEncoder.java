package ontology.solvers.backend.z3.encoder;

import com.microsoft.z3.Context;

import checkers.inference.solver.backend.z3.Z3BitVectorFormatTranslator;
import checkers.inference.solver.backend.z3.encoder.Z3BitVectorSubtypeConstraintEncoder;
import checkers.inference.solver.frontend.Lattice;

public class OntologyZ3BitVectorSubtypeConstraintEncoder extends Z3BitVectorSubtypeConstraintEncoder {

    public OntologyZ3BitVectorSubtypeConstraintEncoder(Lattice lattice, Context context,
            Z3BitVectorFormatTranslator z3BitVectorFormatTranslator) {
        super(lattice, context, z3BitVectorFormatTranslator);
    }

    @Override
    protected boolean isSubtypeSubset() {
        return false;
    }
}
