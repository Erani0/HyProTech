package com.google.common.base;

@FunctionalInterface
public interface Supplier<T> {
    T get();
}
