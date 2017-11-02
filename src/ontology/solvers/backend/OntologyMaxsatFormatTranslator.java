package ontology.solvers.backend;

import java.util.ArrayList;
import java.util.List;


import org.checkerframework.javacutil.AnnotationUtils;
import org.sat4j.core.VecInt;

import checkers.inference.model.CombineConstraint;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Slot;
import checkers.inference.model.VariableSlot;
import checkers.inference.solver.backend.maxsat.MathUtils;
import checkers.inference.solver.backend.maxsat.MaxSatFormatTranslator;
import checkers.inference.solver.backend.maxsat.VectorUtils;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.solver.frontend.VariableCombos;
import ontology.util.OntologyUtils;

public class OntologyMaxsatFormatTranslator extends MaxSatFormatTranslator {

    public OntologyMaxsatFormatTranslator(Lattice lattice) {
        super(lattice);
    }

    @Override
    public VecInt[] serialize(CombineConstraint combineConstraint) {
        return new OntologyCombineVariableCombos(new VecInt[0]).accept(combineConstraint.getTarget(), combineConstraint.getDeclared(), combineConstraint.getResult(), combineConstraint);
    }

    protected class OntologyCombineVariableCombos extends VariableCombos<CombineConstraint, VecInt[]>{
        public OntologyCombineVariableCombos(VecInt[] emptyValue) {
            super(emptyValue);
        }

        protected VecInt[] constant_constant(ConstantSlot target, ConstantSlot decl, VariableSlot result, CombineConstraint constraint) {
           return encodingRules(target, decl, result, constraint);
        }

        protected VecInt[] variable_constant(VariableSlot target, ConstantSlot decl, VariableSlot result, CombineConstraint constraint) {
            return encodingRules(target, decl, result, constraint);
        }

        VecInt[] encodingRules(Slot target, Slot decl, VariableSlot result, CombineConstraint constraint) {
            List<VecInt> resultClauses = new ArrayList<>();

            if (decl instanceof ConstantSlot) {
                ConstantSlot cDecl = (ConstantSlot) decl;
                if (AnnotationUtils.areSame(OntologyUtils.POLY_ONTOLOGY, cDecl.getValue())) {
                    if (target instanceof ConstantSlot) {
                        resultClauses.add(VectorUtils.asVec(
                                MathUtils.mapIdToMatrixEntry(result.getId(), typeToInt.get(((ConstantSlot)target).getValue()), lattice)));
                    } else {
                        VecInt[] equalityClauses = new EqualityVariableCombos(emptyClauses)
                                .accept(result, target, null);
                        return equalityClauses;
                    }
                } else {
                    if (target instanceof ConstantSlot) {
                        //TODO: TBD
                        return defaultAction();
                    } else {
                        if (lattice.allTypes.contains(cDecl.getValue())) {
                            resultClauses.add(VectorUtils.asVec(
                                    MathUtils.mapIdToMatrixEntry(result.getId(), typeToInt.get(cDecl.getValue()), lattice)));
                        }
                    }
                }
            } else {
                return defaultAction();
            }

            return resultClauses.toArray(new VecInt[resultClauses.size()]);
        }
    }
}
