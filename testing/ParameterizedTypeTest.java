interface Iterator<T> {
    T next();
}

abstract class CursorIterator implements Iterator<Double> {
}

abstract class Matrix {
    public void multiply1(CursorIterator it) {
        double x = it.next();
    }

    public void multiply2(CursorIterator it) {
        double x;
        x = it.next();
    }
}
