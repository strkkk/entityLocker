package com.example;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.impl.StripingEntityLocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EntityLockerTest {

    StripingEntityLocker<String> testObj = new StripingEntityLocker<>();

    @Test
    public void testOperationSingleId() throws ExecutionException, TimeoutException {
        AtomicInteger cnt = new AtomicInteger();
        testObj.executeWithLock("1", () -> cnt.incrementAndGet());
        Assertions.assertEquals(1, cnt.get());
    }

    @Test
    public void testOperationWithResultSingleId() throws ExecutionException, TimeoutException {
        Integer res = testObj.executeWithLock("1", () -> 1);
        Assertions.assertEquals(1, res);
    }

    @Test
    public void testExceptionInExecution() {
        ExecutionException ex = assertThrows(ExecutionException.class,
            () -> testObj.executeWithLock("1", () -> {
                throw new IllegalArgumentException();
            }));
        Throwable cause = ex.getCause();
        Assertions.assertTrue(cause instanceof IllegalArgumentException);
    }

    @ParameterizedTest
    @CsvSource({
        ",1,SECONDS",
        "1,-1,SECONDS",
        "1,1,",
    })
    public void testInvalidArgs(String id, long timeoutAmount, TimeUnit unit) {
        Callable<Integer> integerCallable = () -> 1;
        assertThrows(IllegalArgumentException.class,
            () -> testObj.executeWithLock(id, integerCallable, timeoutAmount, unit));
    }

    @ParameterizedTest
    @CsvSource({
        ",1,SECONDS",
        "1,-1,SECONDS",
        "1,1,",
    })
    public void testInvalidArgs2(String id, long timeoutAmount, TimeUnit unit) {
        Runnable runnable = () -> {};
        assertThrows(IllegalArgumentException.class,
            () -> testObj.executeWithLock(id, runnable, timeoutAmount, unit));
    }
}
