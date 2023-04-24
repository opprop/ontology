package ontology.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface Ontology {
    /** default qualifier is the top qualifier: Ontology(values=[OntologyValue.TOP]) */
    OntologyValue[] values() default {OntologyValue.TOP};
}
