package ontology.util;

import checkers.inference.model.SubtypeConstraint;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;

public class ViolatedConsDiagnostic {
    SubtypeConstraint subtypeConstraint;
    AnnotationMirror inferredSubtype;
    AnnotationMirror inferredSupertype;
    List<AnnotationMirror> subtypeSolutions;
    List<AnnotationMirror> supertypeSolutions;

    public ViolatedConsDiagnostic(
            SubtypeConstraint subtypeConstraint,
            AnnotationMirror subtype,
            AnnotationMirror supertype) {
        this.subtypeConstraint = subtypeConstraint;
        this.inferredSubtype = subtype;
        this.inferredSupertype = supertype;
        subtypeSolutions = Collections.emptyList();
        supertypeSolutions = Collections.emptyList();
    }

    public void setSubtypeSolutions(List<AnnotationMirror> subtypeSolutions) {
        this.subtypeSolutions = subtypeSolutions;
    }

    public void setSupertypeSolutions(List<AnnotationMirror> supertypeSolutions) {
        this.supertypeSolutions = supertypeSolutions;
    }

    @Override
    public String toString() {
        return subtypeConstraint
                + "\tsubtype: "
                + inferredSubtype
                + "\tsupertype: "
                + inferredSupertype
                + "\t\n subtypeSolutions: "
                + subtypeSolutions
                + "\t\n supertypeSolutions: "
                + supertypeSolutions;
    }
}
