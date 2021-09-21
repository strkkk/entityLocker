package com.example;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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
    public void testOperationReentrant() throws ExecutionException, TimeoutException {
        List<String> list = new ArrayList<>();
        testObj.executeWithLock("1", () -> {
            try {
                testObj.executeWithLock("1", () -> { list.add("1");});
            } catch (Exception e) {
                fail();
            }
            list.add("2");
        });
        Assertions.assertEquals(List.of("1", "2"), list);
    }

    @Test
    public void testTimeout() {
        AtomicBoolean flag = new AtomicBoolean();
        ForkJoinPool.commonPool().execute(() -> {
            try {
                testObj.executeWithLock("1", () -> {
                    flag.set(true);
                    while(flag.get()){
                        // wait
                    }
                });
            } catch (Exception e) {
                fail();
            }
        });

        while(!flag.get()) {
            // wait for first thread to obtain lock
        }
        assertThrows(TimeoutException.class,
            () -> testObj.executeWithLock("1", () -> {}, 1, TimeUnit.MILLISECONDS));
        flag.set(false);
    }

    @Test
    public void testConcurrentOnDifferentIds() throws ExecutionException, TimeoutException {
        // better way is to use proper tools, e.g. jcstress
        AtomicBoolean flag = new AtomicBoolean();
        ForkJoinPool.commonPool().execute(() -> {
            try {
                testObj.executeWithLock("1", () -> {
                    flag.set(true);
                    while(flag.get()){
                        // wait
                    }
                });
            } catch (Exception e) {
                fail();
            }
        });
        while(!flag.get()) {
            // wait for first thread to obtain lock
        }
        List<String> list = new ArrayList<>();
        testObj.executeWithLock("2", () -> {list.add("123");}, 1, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(List.of("123"), list);
        flag.set(false);
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
