package com.example;

import com.example.impl.StripingEntityLocker;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

@JCStressTest
@Outcome(id = "4, 2", expect = Expect.ACCEPTABLE, desc = "OK, thread 2 was first")
@Outcome(id = "2, 4", expect = Expect.ACCEPTABLE, desc = "OK, thread 1 was first")
@State
public class LockerReentrantTest {
    private int x;
    private final String id = "1";

    private StripingEntityLocker<String> testObj = new StripingEntityLocker<>();

    @Actor
    public void actor1(II_Result r) {
        Integer result;
        try {
            result = testObj.executeWithLock(id, () -> {
                try {
                    testObj.executeWithLock(id, () -> {
                        x++;
                    });
                    return ++x;
                } catch (Exception e) {
                    return  -1;
                }
            });
        } catch (Exception e) {
            result = -1;
        }
        r.r1 = result;
    }

    @Actor
    public void actor2(II_Result r) {
        Integer result;
        try {
            result = testObj.executeWithLock(id, () -> {
                try {
                    testObj.executeWithLock(id, () -> {
                        x++;
                    });
                    return ++x;
                } catch (Exception e) {
                    return  -1;
                }
            });
        } catch (Exception e) {
            result = -1;
        }
        r.r2 = result;
    }
}
