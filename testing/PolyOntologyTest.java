import ontology.qual.Ontology;
import ontology.qual.OntologyValue;

public class PolyOntologyTest {
    @Ontology(values = {OntologyValue.VELOCITY_3D}) Vector externalVelocity;

    @Ontology(values = {OntologyValue.FORCE_3D}) Vector externalForce;

    public void applyVelocity(Vector velocity) {
        // :: fixable-error: (assignment.type.incompatible)
        @Ontology(values = {OntologyValue.VELOCITY_3D}) Vector res = externalVelocity.add(velocity);
    }

    public void applyForce(Vector force) {
        // :: fixable-error: (assignment.type.incompatible)
        @Ontology(values = {OntologyValue.FORCE_3D}) Vector res = externalForce.add(force);
        // :: fixable-error: (assignment.type.incompatible)
        @Ontology(values = {OntologyValue.FORCE_3D}) Vector max = Vector.max(externalForce, force);
    }
}

class Vector {
    int x;
    int y;
    int z;

    public @Ontology(values = {OntologyValue.POLY}) Vector add(
            @Ontology(values = {OntologyValue.POLY}) Vector this,
            @Ontology(values = {OntologyValue.POLY}) Vector other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    public static @Ontology(values = {OntologyValue.POLY}) Vector max(
            @Ontology(values = {OntologyValue.POLY}) Vector a,
            @Ontology(values = {OntologyValue.POLY}) Vector b) {
        int aLength = a.length();
        int bLength = b.length();

        // test lub of polys
        if (aLength > bLength) {
            return a;
        }
        return b;
    }

    public int length() {
        return x * x + y * y + z * z;
    }
}
