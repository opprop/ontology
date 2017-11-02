package ontology.solvers.backend.z3;

import org.checkerframework.javacutil.AnnotationUtils;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;

import checkers.inference.model.ConstantSlot;
import checkers.inference.model.EqualityConstraint;
import checkers.inference.model.InequalityConstraint;
import checkers.inference.model.PreferenceConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.solver.backend.z3.Z3BitVectorCodec;
import checkers.inference.solver.backend.z3.Z3BitVectorFormatTranslator;
import checkers.inference.solver.frontend.Lattice;
import ontology.util.OntologyUtils;

public class OntologyZ3FormatTranslator extends Z3BitVectorFormatTranslator {

    public OntologyZ3FormatTranslator(Lattice lattice) {
        super(lattice);
    }

    @Override
    protected Z3BitVectorCodec createZ3BitVectorCodec() {
        return new OntologyZ3BitVectorCodec();
    }

    @Override
    public BoolExpr serialize(SubtypeConstraint constraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : constraint.getSlots()) {
            if (isPolyOntology(slot)) {
                return EMPTY_VALUE;
            }
        }

        // Soft constraints for subtypeConstraint
        Slot subtypeSlot = constraint.getSubtype();
        Slot supertypeSlot = constraint.getSupertype();
        BitVecExpr subtypeBV = subtypeSlot.serialize(this);
        BitVecExpr supertypeBV = supertypeSlot.serialize(this);
        int weight = 1;
        if (subtypeSlot instanceof ConstantSlot) {
            weight = 2;
        }

        addSoftConstraint(context.mkEq(subtypeBV, supertypeBV), weight, "default");
        return super.serialize(constraint);
    }

     @Override
    protected boolean isSubtypeSubSet() {
        return false;
    }

    @Override
    public BoolExpr serialize(EqualityConstraint constraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : constraint.getSlots()) {
            if (isPolyOntology(slot)) {
                return EMPTY_VALUE;
            }
        }
        return super.serialize(constraint);
    }

    @Override
    public BoolExpr serialize(InequalityConstraint constraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : constraint.getSlots()) {
            if (isPolyOntology(slot)) {
                return EMPTY_VALUE;
            }
        }
        return super.serialize(constraint);
    }

    @Override
    public BoolExpr serialize(PreferenceConstraint preferenceConstraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : preferenceConstraint.getSlots()) {
            if (isPolyOntology(slot)) {
                return EMPTY_VALUE;
            }
        }
        return super.serialize(preferenceConstraint);
    }

    protected boolean isPolyOntology(Slot slot) {
        return slot instanceof ConstantSlot &&
                AnnotationUtils.areSameIgnoringValues(((ConstantSlot) slot).getValue(), OntologyUtils.POLY_ONTOLOGY);
    }
}
