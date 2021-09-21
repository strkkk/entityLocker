package com.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class LockWrapper {
    private final AtomicInteger useCount = new AtomicInteger();
    private final ReentrantLock delegate = new ReentrantLock();

    public void incrementUsageCount() {
        useCount.incrementAndGet();
    }

    public void decrementUsageCount() {
        useCount.decrementAndGet();
    }

    public boolean isInUse() {
        return useCount.get() != 0;
    }

    public boolean tryLock(long timeOut, TimeUnit timeUnit) throws InterruptedException {
        return delegate.tryLock(timeOut, timeUnit);
    }

    public void unlock() {
        delegate.unlock();
    }
}
