package com.example.impl;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import com.example.LockerUtil;
import com.example.EntityLocker;
import com.example.ExecutionException;

public final class SimpleEntityLocker<T> implements EntityLocker<T> {
    private final Map<T, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final long defaultTimeAmount;
    private final TimeUnit timeUnit;

    public SimpleEntityLocker() {
        this(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public SimpleEntityLocker(long defaultTimeAmount, TimeUnit timeUnit) {
        LockerUtil.checkNotNull(timeUnit, "timeUnit");
        if (defaultTimeAmount < 0) {
            throw new IllegalArgumentException("time amount should be greater than 0");
        }
        this.defaultTimeAmount = defaultTimeAmount;
        this.timeUnit = timeUnit;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if protectedCode is null
     */
    @Override
    public void executeWithLock(T id, Runnable protectedCode) throws ExecutionException, TimeoutException {
        LockerUtil.checkNotNull(id, "id");
        LockerUtil.checkNotNull(protectedCode, "protectedCode");
        execute(id, LockerUtil.convert2callable(protectedCode), defaultTimeAmount, timeUnit);
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
        LockerUtil.checkNotNull(id, "id");
        LockerUtil.checkNotNull(protectedCode, "protectedCode");
        LockerUtil.checkNotNull(timeUnit, "timeUnit");
        if (timeAmount < 0) {
            throw new IllegalArgumentException("time amount should be >= 0");
        }
        execute(id, LockerUtil.convert2callable(protectedCode), timeAmount, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if id is null
     * @throws IllegalArgumentException if protectedCode is null
     */
    @Override
    public <R> R executeWithLock(T id, Callable<R> protectedCode) throws ExecutionException, TimeoutException {
        LockerUtil.checkNotNull(id, "id");
        LockerUtil.checkNotNull(protectedCode, "protectedCode");
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
        LockerUtil.checkNotNull(id, "id");
        LockerUtil.checkNotNull(protectedCode, "protectedCode");
        LockerUtil.checkNotNull(timeUnit, "timeUnit");
        if (timeAmount < 0) {
            throw new IllegalArgumentException("time amount should be greater than 0");
        }
        return execute(id, protectedCode, timeAmount, timeUnit);
    }

    private <R> R execute(T id, Callable<R> protectedCode, long timeAmount, TimeUnit timeUnit) throws ExecutionException, TimeoutException {
        ReentrantLock lock = locks.computeIfAbsent(id, key -> new ReentrantLock());
        boolean locked = false;
        try {
            locked = lock.tryLock(timeAmount, timeUnit);
            if (locked) {
                return protectedCode.call();
            }
        } catch (Exception ex) {
            throw new ExecutionException("Exception during execution with lock", ex);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
        throw new TimeoutException("Operation cannot be executed due to timeout");
    }
}
