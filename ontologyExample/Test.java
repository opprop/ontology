import ontology.qual.Ontology;
import ontology.qual.OntologyValue;
import ontology.qual.PolyOntology;

public class Test {
    @Ontology(values={OntologyValue.VELOCITY_3D}) Vector externalVelocity;
    @Ontology(values={OntologyValue.FORCE_3D}) Vector externalForce;

    public void applyVelocity(Vector velocity) {
        externalVelocity.add(velocity);
    }

    public void applyForce(Vector force) {
        externalForce.add(force);
    }
}

class Vector {
    int x;
    int y;
    int z;
    public @PolyOntology Vector add(@PolyOntology Vector this, @PolyOntology Vector other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }
}