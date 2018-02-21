package ontology.solvers.backend.z3;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import checkers.inference.solver.backend.z3.Z3BitVectorCodec;
//import checkers.inference.solver.backend.z3backend.Z3BitVectorCodec;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;

public class OntologyZ3BitVectorCodec implements Z3BitVectorCodec {

    private final int domainSize;

    private final Map<OntologyValue, Integer> ontologyValueEncodingMap;


    public OntologyZ3BitVectorCodec() {
        this.domainSize = OntologyValue.values.length - 2;

        // Check domain size limit.
        // TODO: Extend supportability by using BitSet and BigInteger if needs.
        if (domainSize > Integer.SIZE) {
            ErrorReporter.errorAbort("Crruently Ontology Z3 BitVectorCodec implementation cannot support domain size larger than " + Integer.SIZE);
        }

        Map<OntologyValue, Integer> tempMap = new EnumMap<>(OntologyValue.class);
        for (OntologyValue ontologyValue : OntologyValue.values) {
            tempMap.put(ontologyValue, encodeOntologyValue(ontologyValue));
        }
        ontologyValueEncodingMap = Collections.unmodifiableMap(tempMap);
    }

    @Override
    public int getFixedBitVectorSize() {
        return domainSize;
    }

    @Override
    public BigInteger encodeConstantAM(AnnotationMirror am) {
        if (!AnnotationUtils.areSameIgnoringValues(am, OntologyUtils.ONTOLOGY)) {
            return BigInteger.valueOf(-1);
        }

        if (OntologyUtils.isOntologyTop(am)) {
            return BigInteger.valueOf(0);
        }

        OntologyValue[] values = OntologyUtils.getOntologyValues(am);
        int encode = 0;
        for (OntologyValue ontologyValue : values) {
            encode |= ontologyValueEncodingMap.get(ontologyValue);
        }
        return BigInteger.valueOf(encode);
    }

    @Override
    public AnnotationMirror decodeNumeralValue(BigInteger numeralValue, ProcessingEnvironment processingEnvironment) {
        int intNumberalValue = numeralValue.intValue();
        Set<OntologyValue> ontologyValues = EnumSet.noneOf(OntologyValue.class);

        for (Entry<OntologyValue, Integer> entry : ontologyValueEncodingMap.entrySet()) {
            int ontologyNumeralValue = entry.getValue();
            OntologyValue ontologyValue = entry.getKey();
            if ((ontologyNumeralValue & intNumberalValue) == ontologyNumeralValue) {
                if (ontologyValue == OntologyValue.TOP && ontologyNumeralValue != intNumberalValue) {
                    continue;
                }
                ontologyValues.add(ontologyValue);
                if (ontologyNumeralValue == intNumberalValue) {
                    break;
                }
            }
        }

        if (OntologyValue.isEqualToBottom(ontologyValues)) {
            return OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                    OntologyValue.BOTTOM);
        }

        return OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                ontologyValues.toArray(new OntologyValue[ontologyValues.size()]));
    }

    protected int encodeOntologyValue(OntologyValue ontologyValue) {
        int encode;

        if (ontologyValue == OntologyValue.TOP) {
            return 0;
        }

        if (ontologyValue == OntologyValue.BOTTOM) {
            // Start from 0, then left-shift and accumulated by bit OR.
            encode = 0;
            for (int i = 0; i < domainSize; i ++) {
                encode = encode << 1;
                encode = encode | 1;
            }
            return encode;
        }

        int oridnal = OntologyValue.singleRealValueToOrdinal.get(ontologyValue);
        // Start from 1, then left-shift it to corresponding binary position.
        encode = 1;
        for (int i = 0; i < oridnal; i ++) {
            encode = encode << 1;
        }
        return encode;
    }
}
