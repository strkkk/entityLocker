package com.example;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface EntityLocker<T> {

    /**
     * Execute given protected code on entity.
     *
     * @param id entity id
     * @param protectedCode code to run
     * @throws ExecutionException if some error during execution occurs
     * @throws TimeoutException if lock couldn't be acquired in time amount specified by implementation
     */
    void executeWithLock(T id, Runnable protectedCode) throws ExecutionException, TimeoutException;

    /**
     * Execute given protected code on entity with specified timeout.
     *
     * @param id entity id
     * @param protectedCode code to run
     * @param timeAmount time amount for timeout
     * @param timeUnit time unit for timeout
     * @throws ExecutionException if some error during execution occurs
     * @throws TimeoutException if lock couldn't be acquired in time amount specified by implementation
     */
    void executeWithLock(T id, Runnable protectedCode, long timeAmount, TimeUnit timeUnit) throws ExecutionException, TimeoutException;

    /**
     *
     * @param id entity id
     * @param protectedCode code to run
     * @param <R> result type
     * @return result of protected code execution
     * @throws ExecutionException if some error during execution occurs
     * @throws TimeoutException if lock couldn't be acquired in time amount specified by implementation
     */
    <R> R executeWithLock(T id, Callable<R> protectedCode) throws ExecutionException, TimeoutException;

    /**
     *
     * @param id entity id
     * @param protectedCode code to run
     * @param timeAmount time amount for timeout
     * @param timeUnit time unit for timeout
     * @param <R> result type
     * @return result of protected code execution
     * @throws ExecutionException if some error during execution occurs
     * @throws TimeoutException if lock couldn't be acquired in time amount specified by implementation
     */
    <R> R executeWithLock(T id, Callable<R> protectedCode, long timeAmount, TimeUnit timeUnit) throws ExecutionException, TimeoutException;
}
