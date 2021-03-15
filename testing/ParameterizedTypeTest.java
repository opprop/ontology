interface Iterator<T> {
    T next();
}

abstract class CursorIterator implements Iterator<Double> {
}

abstract class Matrix {
    public void multiply(CursorIterator it) {
        double x;
        x = it.next();
    }
}
