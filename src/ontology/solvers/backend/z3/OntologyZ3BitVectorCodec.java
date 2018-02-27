package ontology.solvers.backend.z3;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private final Map<OntologyValue, BigInteger> ontologyValueEncodingMap;

    public OntologyZ3BitVectorCodec() {
        this.domainSize = OntologyValue.values.length - 2;

        // Check domain size limit.
        // TODO: Extend supportability by using BitSet and BigInteger if needs.
        if (domainSize > Integer.SIZE) {
            ErrorReporter.errorAbort("Crruently Ontology Z3 BitVectorCodec implementation cannot support domain size larger than " + Integer.SIZE);
        }
        ontologyValueEncodingMap = Collections.unmodifiableMap(createOntologyValueEncodingMap());
    }

    private Map<OntologyValue, BigInteger> createOntologyValueEncodingMap() {
        Map<OntologyValue, BigInteger> encodingMap = new EnumMap<>(OntologyValue.class);
        BigInteger encode = BigInteger.ZERO;
        for (OntologyValue ontologyValue : OntologyValue.values) {
            switch (ontologyValue) {
                case TOP: {
                    // Defensive programming, TOP's encoding is ZERO.
                    encode = BigInteger.ZERO;
                    break;
                }
                case BOTTOM: {
                    encode = BigInteger.ZERO;
                    for (int i = 0; i < OntologyValue.values.length; i ++) {
                        encode = encode.setBit(i);
                    }
                    break;
                }
                default: {
                    int ordinal = OntologyValue.singleRealValueToOrdinal.get(ontologyValue);
                    encode = BigInteger.ZERO.setBit(ordinal);
                }
            }
            encodingMap.put(ontologyValue, encode);
        }
        return encodingMap;
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
        BigInteger encode = BigInteger.ZERO;
        for (OntologyValue ontologyValue : values) {
            encode = encode.or(ontologyValueEncodingMap.get(ontologyValue));
        }
        return encode;
    }

    @Override
    public AnnotationMirror decodeNumeralValue(BigInteger numeralValue, ProcessingEnvironment processingEnvironment) {
        Set<OntologyValue> ontologyValues = EnumSet.noneOf(OntologyValue.class);

        // If numberalValue represents an empty set, then it is TOP.
        if (numeralValue.equals(BigInteger.ZERO)) {
            return OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                    OntologyValue.TOP);
        }

        for (Entry<OntologyValue, BigInteger> entry : ontologyValueEncodingMap.entrySet()) {
            BigInteger ontologyNumeralValue = entry.getValue();
            OntologyValue ontologyValue = entry.getKey();

            if (ontologyValue == OntologyValue.TOP) {
                // Skip the TOP case, as we've alredy checked TOP before.
                continue;
            }

            // If the set represented by numerlValue is a super set of ontologyNumberValue,
            // then the corresponding ontologyValue is belongs to the resulted AM's ontologyValues set.
            if (ontologyNumeralValue.and(numeralValue).equals(ontologyNumeralValue)) {
                ontologyValues.add(ontologyValue);

                // If numeralValue is equal to a single ontologyNumberalValue,
                // then no further searching is needed. We've already found the decoding.
                if (ontologyNumeralValue.equals(numeralValue)) {
                    break;
                }
            }
        }

        // If the resulted ontologyValues express the same meaning of BOTTOM,
        // Just create an AM with BOTTOM value.
        if (OntologyValue.isEqualToBottom(ontologyValues)) {
            return OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                    OntologyValue.BOTTOM);
        }

        return OntologyUtils.createOntologyAnnotationByValues(processingEnvironment,
                ontologyValues.toArray(new OntologyValue[ontologyValues.size()]));
    }
}
