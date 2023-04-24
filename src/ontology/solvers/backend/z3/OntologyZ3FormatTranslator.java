package ontology.solvers.backend.z3;

import checkers.inference.model.ConstantSlot;
import checkers.inference.model.EqualityConstraint;
import checkers.inference.model.InequalityConstraint;
import checkers.inference.model.PreferenceConstraint;
import checkers.inference.model.Slot;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.solver.backend.encoder.ConstraintEncoderFactory;
import checkers.inference.solver.backend.z3.Z3BitVectorCodec;
import checkers.inference.solver.backend.z3.Z3BitVectorFormatTranslator;
import checkers.inference.solver.frontend.Lattice;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;

import ontology.solvers.backend.z3.encoder.OntologyZ3BitVectorConstraintEncoderFactory;
import ontology.util.OntologyUtils;

import org.checkerframework.javacutil.AnnotationUtils;

public class OntologyZ3FormatTranslator extends Z3BitVectorFormatTranslator {

    public OntologyZ3FormatTranslator(Lattice lattice) {
        super(lattice);
    }

    @Override
    protected Z3BitVectorCodec createZ3BitVectorCodec() {
        return new OntologyZ3BitVectorCodec();
    }

    @Override
    protected ConstraintEncoderFactory<BoolExpr> createConstraintEncoderFactory() {
        return new OntologyZ3BitVectorConstraintEncoderFactory(lattice, context, this);
    }

    @Override
    public BoolExpr serialize(SubtypeConstraint constraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode
        // POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : constraint.getSlots()) {
            if (isPolyOntology(slot)) {
                // Return true as empty value, to filter out poly ontology.
                // TODO: Make encoder.emptyValue public, then using that consistent empty value
                // here.
                return context.mkTrue();
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
    public BoolExpr serialize(EqualityConstraint constraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode
        // POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : constraint.getSlots()) {
            if (isPolyOntology(slot)) {
                // Return true as empty value, to filter out poly ontology.
                // TODO: Make encoder.emptyValue public, then using that consistent empty value
                // here.
                return context.mkTrue();
            }
        }
        return super.serialize(constraint);
    }

    @Override
    public BoolExpr serialize(InequalityConstraint constraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode
        // POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : constraint.getSlots()) {
            if (isPolyOntology(slot)) {
                // Return true as empty value, to filter out poly ontology.
                // TODO: Make encoder.emptyValue public, then using that consistent empty value
                // here.
                return context.mkTrue();
            }
        }
        return super.serialize(constraint);
    }

    @Override
    public BoolExpr serialize(PreferenceConstraint preferenceConstraint) {
        // Ignore constraints that contains POLY_ONTOLOGY, as currently we don't encode
        // POLY_ONTOLOGY into the domain.
        // TODO: how to encode POLY?
        for (Slot slot : preferenceConstraint.getSlots()) {
            if (isPolyOntology(slot)) {
                // Return true as empty value, to filter out poly ontology.
                // TODO: Make encoder.emptyValue public, then using that consistent empty value
                // here.
                return context.mkTrue();
            }
        }
        return super.serialize(preferenceConstraint);
    }

    protected boolean isPolyOntology(Slot slot) {
        return slot instanceof ConstantSlot
                && AnnotationUtils.areSame(
                        ((ConstantSlot) slot).getValue(), OntologyUtils.POLY_ONTOLOGY);
    }
}
