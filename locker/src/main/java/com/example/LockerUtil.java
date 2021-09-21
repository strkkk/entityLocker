package com.example;

import java.util.concurrent.Callable;

public final class LockerUtil {

    public static void checkNotNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException("null values are not allowed as" + name);
        }
    }

    public static Callable<Void> convert2callable(Runnable action) {
        return () -> {
            action.run();
            return null;
        };
    }
}
