package ontology.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;


@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface Ontology {
    /** default qualifier is the top qualifier: Ontology(values=[OntologyValue.TOP]) */
    OntologyValue[] values() default {OntologyValue.TOP};
}
