package com.example.impl;

import static com.example.LockerUtil.checkNotNull;
import static com.example.LockerUtil.convert2callable;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import com.example.EntityLocker;
import com.example.ExecutionException;
import com.example.LockWrapper;

public final class StripingEntityLocker<T> implements EntityLocker<T> {

    private static final int DEFAULT_STRIPES = 32;
    private final Map<T, LockWrapper> locks = new ConcurrentHashMap<>();
    private final Lock[] stripes;
    private final long defaultTimeAmount;
    private final TimeUnit timeUnit;

    public StripingEntityLocker() {
        this(Long.MAX_VALUE, TimeUnit.MILLISECONDS, DEFAULT_STRIPES);
    }

    public StripingEntityLocker(long lockTimeout, TimeUnit lockTimeoutUnit) {
        this(lockTimeout, lockTimeoutUnit, DEFAULT_STRIPES);
    }

    public StripingEntityLocker(long defaultTimeAmount, TimeUnit timeUnit, int stripesNum) {
        checkNotNull(timeUnit, "timeUnit");
        if (defaultTimeAmount < 0) {
            throw new IllegalArgumentException("time amount should be >= 0");
        }
        if (stripesNum < 1) {
            throw new IllegalArgumentException("number of stripesNum should be greater than 0");
        }
        this.defaultTimeAmount = defaultTimeAmount;
        this.timeUnit = timeUnit;
        this.stripes = new Lock[stripesNum];
        IntStream.range(0, stripesNum).forEach(idx -> stripes[idx] = new ReentrantLock());
    }

    @Override
    public void executeWithLock(T id, Runnable protectedCode) throws ExecutionException, TimeoutException {
        checkNotNull(id, "id");
        checkNotNull(protectedCode, "protectedCode");
        execute(id, convert2callable(protectedCode), defaultTimeAmount, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if protectedCode is null
     * @throws IllegalArgumentException if time unit is null
     * @throws IllegalArgumentException if time amount is less than zero
     */
    @Override
    public void executeWithLock(T id, Runnable protectedCode, long timeAmount, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
        checkNotNull(id, "id");
        checkNotNull(protectedCode, "protectedCode");
        checkNotNull(timeUnit, "timeUnit");
        if (timeAmount < 0) {
            throw new IllegalArgumentException("time amount should be greater than 0");
        }
        execute(id, convert2callable(protectedCode), timeAmount, timeUnit);
    }

    @Override
    public <R> R executeWithLock(T id, Callable<R> protectedCode) throws ExecutionException, TimeoutException {
        checkNotNull(id, "id");
        checkNotNull(protectedCode, "protectedCode");
        return execute(id, protectedCode, defaultTimeAmount, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if protectedCode is null
     * @throws IllegalArgumentException if time unit is null
     * @throws IllegalArgumentException if time amount is less than zero
     */
    @Override
    public <R> R executeWithLock(T id, Callable<R> protectedCode, long timeAmount, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
        checkNotNull(id, "id");
        checkNotNull(protectedCode, "protectedCode");
        checkNotNull(timeUnit, "timeUnit");
        if (timeAmount < 0) {
            throw new IllegalArgumentException("time amount should be greater than 0");
        }
        return execute(id, protectedCode, timeAmount, timeUnit);
    }

    private <R> R execute(T id, Callable<R> protectedCode, long timeAmount, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
        LockWrapper lock = getKeyLock(id);
        boolean locked = false;
        try {
            locked = lock.tryLock(timeAmount, timeUnit);
            if (locked) {
                return protectedCode.call();
            }
        } catch (Exception ex) {
            throw new ExecutionException("Exception during execution with lock", ex);
        } finally {
            releaseLock(id, lock, locked);
        }
        throw new TimeoutException("Operation cannot be executed due to timeout");
    }

    private void releaseLock(T id, LockWrapper lock, boolean locked) {
        Lock stripeLock = stripes[id.hashCode() % stripes.length];
        stripeLock.lock();
        try {
            if (locked) {
                lock.unlock();
            }
            lock.decrementUsageCount();
            if (!lock.isInUse()) {
                locks.remove(id);
            }
        } finally {
            stripeLock.unlock();
        }
    }

    private LockWrapper getKeyLock(T id) {
        Lock stripeLock = stripes[id.hashCode() % stripes.length];
        stripeLock.lock();
        try {
            LockWrapper result = locks.computeIfAbsent(id, key -> new LockWrapper());
            result.incrementUsageCount();
            return result;
        } finally {
            stripeLock.unlock();
        }
    }
}
