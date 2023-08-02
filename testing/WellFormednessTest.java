import ontology.qual.Ontology;
import ontology.qual.OntologyValue;

public class WellFormednessTest {
    // :: error: (type.invalid.conflicting.elements)
    @Ontology(values = {OntologyValue.POLY, OntologyValue.FORCE_3D}) String invalidPoly;

    // :: error: (type.invalid.conflicting.elements)
    @Ontology(values = {OntologyValue.TOP, OntologyValue.FORCE_3D}) String invalidTop;

    // :: error: (type.invalid.conflicting.elements)
    @Ontology(values = {OntologyValue.BOTTOM, OntologyValue.FORCE_3D}) String invalidBottom;
}
