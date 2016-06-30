package ontology.solvers.classic;

import ontology.util.OntologyUtils;
import ontology.qual.OntologyValue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import org.sat4j.core.VecInt;

import checkers.inference.InferenceMain;
import checkers.inference.model.ConstantSlot;
import checkers.inference.model.Constraint;
import checkers.inference.model.serialization.CnfVecIntSerializer;

public class OntologySerializer extends CnfVecIntSerializer {

    protected final OntologyValue value;

    public OntologySerializer(OntologyValue ontologyValue) {
        super(InferenceMain.getInstance().getSlotManager());
        this.value = ontologyValue;
    }

    @Override
    protected boolean isTop(ConstantSlot constantSlot) {
        AnnotationMirror anno = constantSlot.getValue();
        // TODO: Need to think clear about why return !annoIsPresented
        return !annoIsPresented(anno);
    }

    private boolean annoIsPresented(AnnotationMirror anno) {
        EnumSet<OntologyValue> valuesSet = EnumSet.noneOf(OntologyValue.class);
        valuesSet = EnumSet.copyOf(Arrays.asList(OntologyUtils.getOntologyValues(anno)));
        return valuesSet.contains(value) || valuesSet.contains(OntologyValue.BOTTOM);
    }

    @Override
    public List<VecInt> convertAll(Iterable<Constraint> constraints,
            List<VecInt> results) {
        for (Constraint constraint : constraints) {
                for (VecInt res : constraint.serialize(this)) {
                        results.add(res);
                }
        }
        return results;
    }
}
