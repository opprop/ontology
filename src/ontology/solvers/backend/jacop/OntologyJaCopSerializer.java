package ontology.solvers.backend.jacop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;
import org.jacop.constraints.Constraint;
import org.jacop.core.IntervalDomain;
import org.jacop.core.Store;
import org.jacop.core.Var;
import org.jacop.set.constraints.AeqB;
import org.jacop.set.constraints.AinB;
import org.jacop.set.core.BoundSetDomain;
import org.jacop.set.core.SetVar;

import checkers.inference.model.CombVariableSlot;
import checkers.inference.model.CombineConstraint;
import checkers.inference.model.ComparableConstraint;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.EqualityConstraint;
import checkers.inference.model.ExistentialConstraint;
import checkers.inference.model.ExistentialVariableSlot;
import checkers.inference.model.InequalityConstraint;
import checkers.inference.model.PreferenceConstraint;
import checkers.inference.model.RefinementVariableSlot;
import checkers.inference.model.Serializer;
import checkers.inference.model.SubtypeConstraint;
import checkers.inference.model.VariableSlot;

import constraintsolver.VariableCombos;

import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;

public class OntologyJaCopSerializer implements Serializer<SetVar, Constraint>{

    private final Map<Integer, SetVar> serializedSetVars;
    private final Set<SetVar> variableSetVars;
    private final Store store;
    public static final Constraint EMPTY_CONSTRAINT = EmptyConstraint.getSingleton();

    private final IntervalDomain[] singleOntologyValueDomain;

    private final SubtypeVariableCombos subtypeVariableCombos;
    private final EqualityVariableCombos equalityVariableCombos;

    public OntologyJaCopSerializer(Store store) {
        this.store = store;
        this.serializedSetVars = new HashMap<>();
        this.variableSetVars = new HashSet<>();
        this.subtypeVariableCombos = new SubtypeVariableCombos(EMPTY_CONSTRAINT);
        this.equalityVariableCombos = new EqualityVariableCombos(EMPTY_CONSTRAINT);

        this.singleOntologyValueDomain = new IntervalDomain[OntologyValue.values.length];

        // Initialize singleOntologyValueDomain
        for (OntologyValue ontologyValue : OntologyValue.values) {
            // Encode ontologyValue to Integer. Use Enum.ordinal() as the associate int id for each OntologyValue.
            //TODO: decoupling: oridnal orders
            int ontologyValueId = ontologyValue.ordinal();
            singleOntologyValueDomain[ontologyValueId] = new IntervalDomain(ontologyValueId, ontologyValueId);
        }
    }

    public Set<SetVar> getVariableSetVars() {
        return new HashSet<>(variableSetVars);
    }

    @Override
    public Constraint serialize(SubtypeConstraint constraint) {
        return subtypeVariableCombos.accept(constraint.getSubtype(), constraint.getSupertype(), constraint);
    }

    @Override
    public Constraint serialize(EqualityConstraint constraint) {
        return equalityVariableCombos.accept(constraint.getFirst(), constraint.getSecond(), constraint);
    }

    @Override
    public Constraint serialize(InequalityConstraint constraint) {
        // Can support this. But currently Ontology doesn't have InequalityConstraint.
        //TODO: implement this.
        return EMPTY_CONSTRAINT;
    }

    protected SetVar serializeVarSlot(VariableSlot variableSlot) {
        final int varSlotId = variableSlot.getId();

        // Check cache of serialized SetVars first.
        if (serializedSetVars.containsKey(varSlotId)) {
            return serializedSetVars.get(varSlotId);
        }

        SetVar setVar = new SetVar(
                store,
                String.valueOf(variableSlot.getId()),
                1, OntologyValue.values.length);

        serializedSetVars.put(varSlotId, setVar);
        variableSetVars.add(setVar);

        return setVar;
    }

    protected SetVar serializeConstantSlot(ConstantSlot constantSlot) {
        // Check cache of serializedSetVars first.
        if (serializedSetVars.containsKey(constantSlot.getId())) {
            return serializedSetVars.get(constantSlot.getId());
        }

        AnnotationMirror constantType = constantSlot.getValue();

        IntervalDomain constantDomain = new IntervalDomain();

        if(AnnotationUtils.areSameIgnoringValues(OntologyUtils.POLY_ONTOLOGY, constantType)) {
            constantDomain.addDom(new IntervalDomain(OntologyValue.values.length, OntologyValue.values.length));

        } else if (OntologyUtils.isOntologyBottom(constantType)) {
            // Encode constantDomain as the complete ontology values set.
            constantDomain.addDom(new IntervalDomain(0, OntologyValue.values.length - 1));

        } else if (OntologyUtils.isOntologyTop(constantType)) {
            // Keep constantDomain as the empty domain.

        } else {
            // Encode each OntologyValue into constantDomain.
            EnumSet<OntologyValue> valueSet = EnumSet.noneOf(OntologyValue.class);
            valueSet.addAll(Arrays.asList(OntologyUtils.getOntologyValues(constantType)));
            for (OntologyValue ontologyValue : valueSet) {
                constantDomain.addDom(new IntervalDomain(ontologyValue.ordinal(), ontologyValue.ordinal()));
            }
        }

        SetVar constantSetVar = new SetVar(
                store,
                String.valueOf(constantSlot.getId()),
                new BoundSetDomain(constantDomain, constantDomain));

        serializedSetVars.put(constantSlot.getId(), constantSetVar);
        return constantSetVar;
    }

    protected class SubtypeVariableCombos extends VariableCombos<SubtypeConstraint, Constraint> {

        public SubtypeVariableCombos(Constraint emptyValue) {
            super(emptyValue);
        }

        @Override
        protected Constraint variable_constant(VariableSlot subtypeSlot, ConstantSlot supertypeSlot,
                SubtypeConstraint constraint) {
            SetVar subtypeSet = serializeVarSlot(subtypeSlot);
            SetVar supertypeSet = serializeConstantSlot(supertypeSlot);

            if (OntologyUtils.isOntologyBottom(supertypeSlot.getValue())) {
                // If supertype is bottom, then subtype is also bottom.
                // Just set the subtypeSet domain as the same as supertypeSet domain,
                // and don't create constraint (as we already fixed the subtypeSet domain by supertypeSet domain).
                subtypeSet.setDomain(supertypeSet.dom());
                return EMPTY_CONSTRAINT;
            } else {
                return new AinB(supertypeSet, subtypeSet);
            }
        }

        @Override
        protected Constraint variable_variable(VariableSlot subtypeSlot, VariableSlot supertypeSlot,
                checkers.inference.model.SubtypeConstraint constraint) {
            SetVar subtypeSet = serializeVarSlot(subtypeSlot);
            SetVar supertypeSet = serializeVarSlot(supertypeSlot);

            return new AinB(supertypeSet, subtypeSet);
        }

        @Override
        protected Constraint constant_constant(ConstantSlot subtypeSlot, ConstantSlot supertypeSlot,
                SubtypeConstraint constraint) {
            //TODO: add check of const_const subtype relationship here.
            return EMPTY_CONSTRAINT;
        }

        @Override
        protected Constraint constant_variable(ConstantSlot subtypeSlot, VariableSlot supertypeSlot,
                SubtypeConstraint constraint) {
            SetVar subtypeSet = serializeConstantSlot(subtypeSlot);
            SetVar supertypeSet = serializeVarSlot(supertypeSlot);

            if (OntologyUtils.isOntologyTop(subtypeSlot.getValue())) {
                // If subtype is top, then supertype is also bottom.
                // Just set the supertypeSet domain as the same as subtypeSet domain,
                // and don't create constraint (as we already fixed the supertypeSet domain by subtypeSet domain).
                supertypeSet.setDomain(subtypeSet.dom());
                return EMPTY_CONSTRAINT;
            } else {
                return new AinB(supertypeSet, subtypeSet);
            }
        }
    }

    protected class EqualityVariableCombos extends VariableCombos<EqualityConstraint, Constraint> {

        public EqualityVariableCombos(Constraint emptyValue) {
            super(emptyValue);
        }

        @Override
        protected Constraint constant_constant(ConstantSlot slot1, ConstantSlot slot2, EqualityConstraint constraint) {
            //TODO: add equality check here.
            return EMPTY_CONSTRAINT;
        }

        @Override
        protected Constraint constant_variable(ConstantSlot constantSlot, VariableSlot variableSlot, EqualityConstraint constraint) {
            SetVar constantSetVar = serializeConstantSlot(constantSlot);
            SetVar variableSetVar = serializeVarSlot(variableSlot);
            // Fixed the variableSetVar domain by constantSetVar domain.
            variableSetVar.setDomain(constantSetVar.dom());
            return EMPTY_CONSTRAINT;
        }

        @Override
        protected Constraint variable_constant(VariableSlot variableSlot, ConstantSlot constantSlot, EqualityConstraint constraint) {
            SetVar constantSetVar = serializeVarSlot(variableSlot);
            SetVar variableSetVar = serializeConstantSlot(constantSlot);

            assert constantSetVar != null && variableSetVar != null :
                "SubtypeSet and supertypeSet Setvars should be serialized before serializing any constraint!";

            // Fixed the variableSetVar domain by constantSetVar domain.
            variableSetVar.setDomain(constantSetVar.dom());
            return EMPTY_CONSTRAINT;
        }

        @Override
        protected Constraint variable_variable(VariableSlot slot1, VariableSlot slot2, EqualityConstraint constraint) {
            SetVar setVar1 = serializeVarSlot(slot1);
            SetVar setVar2 = serializeVarSlot(slot2);

            return new AeqB(setVar1, setVar2);
        }
    }

    private static class EmptyConstraint extends Constraint {

        private static EmptyConstraint singleton;

        public static EmptyConstraint getSingleton() {
            if (singleton == null) {
                singleton = new EmptyConstraint();
            }
            return singleton;
        }

        private EmptyConstraint() {
        }

        @Override
        public ArrayList<Var> arguments() {
            return null;
        }

        @Override
        public void consistency(Store arg0) {
        }

        @Override
        public int getConsistencyPruningEvent(Var arg0) {
            return 0;
        }

        @Override
        public void impose(Store arg0) {
        }

        @Override
        public void increaseWeight() {
        }

        @Override
        public void removeConstraint() {
        }

        @Override
        public boolean satisfied() {
            return false;
        }

        @Override
        public String toString() {
            return null;
        }
    }

    @Override
    public Constraint serialize(ExistentialConstraint constraint) {
        // Not support ExistentialConstraint.
        return EMPTY_CONSTRAINT;
    }

    @Override
    public Constraint serialize(ComparableConstraint comparableConstraint) {
        // Not supported.
        return EMPTY_CONSTRAINT;
    }

    @Override
    public Constraint serialize(PreferenceConstraint preferenceConstraint) {
        // Not supported.
        return EMPTY_CONSTRAINT;
    }

    @Override
    public Constraint serialize(CombineConstraint combineConstraint) {
        // Not supported.
        return EMPTY_CONSTRAINT;
    }

    @Override
    public SetVar serialize(ConstantSlot slot) {
        return null;
    }

    @Override
    public SetVar serialize(VariableSlot slot) {
        return null;
    }

    @Override
    public SetVar serialize(ExistentialVariableSlot slot) {
        return null;
    }

    @Override
    public SetVar serialize(RefinementVariableSlot slot) {
        return null;
    }

    @Override
    public SetVar serialize(CombVariableSlot slot) {
        return null;
    }
}
