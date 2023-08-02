// A corner case which is not handled by variable annotator and should be applied with defaults.
// If an anonymous class is created from a raw interface, the type argument of the raw type
// is substituted to wildcard. Take the followin case for instance, the anonymous class is
//          class <anonymous> implements Comparator<? extends Object>
// In this case the default is applied to the upper/lower bound

import java.util.Comparator;

abstract class Demo {

    void foo() {
        new Comparator() {
            public int compare(Object o1, Object o2) {
                return 1;
            }
        };
    }
}
