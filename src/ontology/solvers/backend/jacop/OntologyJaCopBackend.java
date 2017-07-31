package ontology.solvers.backend.jacop;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.framework.type.QualifierHierarchy;
import org.jacop.constraints.Constraint;
import org.jacop.core.Domain;
import org.jacop.core.IntDomain;
import org.jacop.core.Store;
import org.jacop.core.ValueEnumeration;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleSelect;
import org.jacop.search.SimpleSolutionListener;
import org.jacop.set.core.SetDomainValueEnumeration;
import org.jacop.set.core.SetVar;
import org.jacop.set.search.IndomainSetMax;
import org.jacop.set.search.MinGlbCard;

import checkers.inference.model.Serializer;
import checkers.inference.model.Slot;
import constraintsolver.BackEnd;
import constraintsolver.Lattice;
import ontology.qual.OntologyValue;
import ontology.util.OntologyUtils;

public class OntologyJaCopBackend extends BackEnd<SetVar, Constraint>{

    private final Store store;
    private final Set<SetVar> solvingVars;
    private final OntologyJaCopSerializer jaCopSerializer;

    public OntologyJaCopBackend(Map<String, String> configuration, Collection<Slot> slots,
            Collection<checkers.inference.model.Constraint> constraints, QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment, Serializer<SetVar, Constraint> realSerializer,
            Lattice lattice) {
        super(configuration, slots, constraints, qualHierarchy, processingEnvironment, realSerializer, lattice);

        this.store = new Store();
        this.solvingVars = new HashSet<>();
        // This is tricky. 
        // I don't use the realSerializer in JaCopBackend, instead I use a specific jaCopSerializer.
        // This is because there is a strong coupling between jaCopBackend and jaCopSerializer
        // --- they have to share the same Store (I don't know why JaCop designed as store is 
        // neccessary when creating a SetVar, but, that is the fact I have to compromise.)
        // Currently I have no other better idea of how to workaround this...
        this.jaCopSerializer = new OntologyJaCopSerializer(store);
    }

    @Override
    public Map<Integer, AnnotationMirror> solve() {
        Map<Integer, AnnotationMirror> result = new HashMap<>();
        convertAll();

        Search<SetVar> search = new DepthFirstSearch<>();
        SetVar[] solvingVarsArr = solvingVars.toArray(new SetVar [solvingVars.size()]);

        SelectChoicePoint<SetVar> selectChoicePoint = new SimpleSelect<SetVar>(
                solvingVarsArr,
                new MinGlbCard<SetVar>(), 
                new MinGlbCard<SetVar>(),
                new IndomainSetMax<SetVar>());
        search.setSolutionListener(new SimpleSolutionListener<SetVar>());
        search.getSolutionListener().recordSolutions(true);
        boolean satisfiable = search.labeling(store, selectChoicePoint);

        if (satisfiable) {
           result = decodeSolution(solvingVarsArr, search.getSolution());
        } else {
            System.out.println("Not Solvable!");
        }
        return result;
    }

    @Override
    public void convertAll() {
        for (checkers.inference.model.Constraint constraint : constraints) {
            collectVarSlots(constraint);
            Constraint setConstraint = constraint.serialize(jaCopSerializer);
            if (!OntologyJaCopSerializer.EMPTY_CONSTRAINT.equals(setConstraint)) {
                store.impose(setConstraint);
            }
        }
        solvingVars.addAll(jaCopSerializer.getVariableSetVars());
    }

    protected Map<Integer, AnnotationMirror> decodeSolution(SetVar[] solvingVars, Domain[] solution) {
        Map<Integer, AnnotationMirror> decodedSolution = new HashMap<>();

        for (int i = 0; i < solution.length; i++) {
            EnumSet<OntologyValue> ontologyValues = EnumSet.noneOf(OntologyValue.class);
            SetDomainValueEnumeration valueEnumeration = (SetDomainValueEnumeration) solution[i].valueEnumeration();
            while(valueEnumeration.hasMoreElements()) {
                //TODO: decoupling setDomain encoding rule with Enum.ordinal()
                IntDomain setElement = valueEnumeration.nextSetElement();
                ValueEnumeration intValueEnumeration = setElement.valueEnumeration();
                while (intValueEnumeration.hasMoreElements()) {
                    int ontologyValueId = intValueEnumeration.nextElement();
                    //TODO: tricky, this is just to avoid encoding polyOntology.
                    // Need more thinking on how to encoding polyOntology.
                    if (ontologyValueId >= OntologyValue.values.length) {
                        continue;
                    }
                    ontologyValues.add(OntologyValue.values[ontologyValueId]);
                }
            }

            Integer slotId = Integer.valueOf(solvingVars[i].id());
            if (ontologyValues.isEmpty()) {
                ontologyValues.add(OntologyValue.TOP);
            } else if (ontologyValues.contains(OntologyValue.BOTTOM) || ontologyValues.size() == OntologyValue.values.length) {
                ontologyValues.clear();
                ontologyValues.add(OntologyValue.BOTTOM);
            }

            AnnotationMirror ontologyAnnotation = OntologyUtils.createOntologyAnnotationByValues(
                    processingEnvironment, ontologyValues.toArray(new OntologyValue[ontologyValues.size()]));

            decodedSolution.put(slotId, ontologyAnnotation);
        }

        return decodedSolution;
    }
}
