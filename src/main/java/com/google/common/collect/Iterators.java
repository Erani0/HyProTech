package com.google.common.collect;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Iterators {
    private Iterators() {
    }

    public static <T> Iterator<T> filter(Iterator<T> unfiltered, Predicate<? super T> predicate) {
        return new Iterator<>() {
            private T nextItem;
            private boolean nextItemSet;

            @Override
            public boolean hasNext() {
                if (nextItemSet) {
                    return true;
                }
                while (unfiltered.hasNext()) {
                    T candidate = unfiltered.next();
                    if (predicate.test(candidate)) {
                        nextItem = candidate;
                        nextItemSet = true;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T result = nextItem;
                nextItem = null;
                nextItemSet = false;
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <F, T> Iterator<T> transform(Iterator<F> fromIterator, Function<? super F, ? extends T> function) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return fromIterator.hasNext();
            }

            @Override
            public T next() {
                return function.apply(fromIterator.next());
            }

            @Override
            public void remove() {
                fromIterator.remove();
            }
        };
    }
}
