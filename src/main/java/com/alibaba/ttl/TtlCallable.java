package com.alibaba.ttl;

import com.alibaba.ttl.spi.TtlAttachments;
import com.alibaba.ttl.spi.TtlAttachmentsDelegate;
import com.alibaba.ttl.spi.TtlEnhanced;
import com.alibaba.ttl.spi.TtlWrapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.ttl.TransmittableThreadLocal.Transmitter.*;

/**
 * {@link TtlCallable} decorate {@link Callable}, so as to get {@link TransmittableThreadLocal}
 * and transmit it to the time of {@link Callable} execution, needed when use {@link Callable} to thread pool.
 * <p>
 * Use factory method {@link #get(Callable)} to get decorated instance.
 * <p>
 * Other TTL Wrapper for common {@code Functional Interface} see {@link TtlWrappers}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see com.alibaba.ttl.threadpool.TtlExecutors
 * @see TtlWrappers
 * @see java.util.concurrent.Executor
 * @see java.util.concurrent.ExecutorService
 * @see java.util.concurrent.ThreadPoolExecutor
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 * @see java.util.concurrent.Executors
 * @see java.util.concurrent.CompletionService
 * @see java.util.concurrent.ExecutorCompletionService
 * @since 0.9.0
 */
public final class TtlCallable<V> implements Callable<V>, TtlWrapper<Callable<V>>, TtlEnhanced, TtlAttachments {
    // 保存父线程的 ThreadLocal 快照
    private final AtomicReference<Object> capturedRef;
    // 实际执行任务
    private final Callable<V> callable;
    // 判断是否执行完，清除任务所保存的 ThreadLocal 快照
    private final boolean releaseTtlValueReferenceAfterCall;

    private TtlCallable(@NonNull Callable<V> callable, boolean releaseTtlValueReferenceAfterCall) {
        // 1.创建时， 从 Transmitter 抓取快照
        this.capturedRef = new AtomicReference<Object>(capture());
        this.callable = callable;
        this.releaseTtlValueReferenceAfterCall = releaseTtlValueReferenceAfterCall;
    }

    /**
     * wrap method {@link Callable#call()}.
     */
    @Override
    public V call() throws Exception {
        Object captured = capturedRef.get();
        // 如果 releaseTtlValueReferenceAfterCall 为 true，则在执行线程取出快照后清除。
        if (captured == null || releaseTtlValueReferenceAfterCall && !capturedRef.compareAndSet(captured, null)) {
            throw new IllegalStateException("TTL value reference is released after call!");
        }
        // 2.使用 Transmitter 将快照重做到当前执行线程，并将原来的值取出
        Object backup = replay(captured);
        try {
            // 3.执行任务
            return callable.call();
        } finally {
            // 4.Transmitter 重新将原值放回执行线程
            restore(backup);
        }
    }

    /**
     * return the original/underneath {@link Callable}.
     */
    @NonNull
    public Callable<V> getCallable() {
        return unwrap();
    }

    /**
     * unwrap to the original/underneath {@link Callable}.
     *
     * @see TtlUnwrap#unwrap(Object)
     * @since 2.11.4
     */
    @NonNull
    @Override
    public Callable<V> unwrap() {
        return callable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TtlCallable<?> that = (TtlCallable<?>) o;

        return callable.equals(that.callable);
    }

    @Override
    public int hashCode() {
        return callable.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " - " + callable.toString();
    }

    /**
     * Factory method, wrap input {@link Callable} to {@link TtlCallable}.
     * <p>
     * This method is idempotent.
     *
     * @param callable input {@link Callable}
     * @return Wrapped {@link Callable}
     */
    @Nullable
    public static <T> TtlCallable<T> get(@Nullable Callable<T> callable) {
        return get(callable, false);
    }


    /**
     * Factory method, wrap input {@link Callable} to {@link TtlCallable}.
     * <p>
     * This method is idempotent.
     *
     * @param callable                          input {@link Callable}
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @return Wrapped {@link Callable}
     */
    @Nullable
    public static <T> TtlCallable<T> get(@Nullable Callable<T> callable, boolean releaseTtlValueReferenceAfterCall) {
        return get(callable, releaseTtlValueReferenceAfterCall, false);
    }

    /**
     * Factory method, wrap input {@link Callable} to {@link TtlCallable}.
     * <p>
     * This method is idempotent.
     *
     * @param callable                          input {@link Callable}
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @param idempotent                        is idempotent or not. {@code true} will cover up bugs! <b>DO NOT</b> set, only when you know why.
     * @return Wrapped {@link Callable}
     */
    @Nullable
    public static <T> TtlCallable<T> get(@Nullable Callable<T> callable, boolean releaseTtlValueReferenceAfterCall, boolean idempotent) {
        if (null == callable) return null;

        if (callable instanceof TtlEnhanced) {
            // avoid redundant decoration, and ensure idempotency
            if (idempotent) return (TtlCallable<T>) callable;
            else throw new IllegalStateException("Already TtlCallable!");
        }
        return new TtlCallable<T>(callable, releaseTtlValueReferenceAfterCall);
    }

    /**
     * wrap input {@link Callable} Collection to {@link TtlCallable} Collection.
     *
     * @param tasks task to be wrapped
     * @return Wrapped {@link Callable}
     */
    @NonNull
    public static <T> List<TtlCallable<T>> gets(@Nullable Collection<? extends Callable<T>> tasks) {
        return gets(tasks, false, false);
    }

    /**
     * wrap input {@link Callable} Collection to {@link TtlCallable} Collection.
     *
     * @param tasks                             task to be wrapped
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @return Wrapped {@link Callable}
     */
    @NonNull
    public static <T> List<TtlCallable<T>> gets(@Nullable Collection<? extends Callable<T>> tasks, boolean releaseTtlValueReferenceAfterCall) {
        return gets(tasks, releaseTtlValueReferenceAfterCall, false);
    }

    /**
     * wrap input {@link Callable} Collection to {@link TtlCallable} Collection.
     *
     * @param tasks                             task to be wrapped
     * @param releaseTtlValueReferenceAfterCall release TTL value reference after run, avoid memory leak even if {@link TtlRunnable} is referred.
     * @param idempotent                        is idempotent or not. {@code true} will cover up bugs! <b>DO NOT</b> set, only when you know why.
     * @return Wrapped {@link Callable}
     */
    @NonNull
    public static <T> List<TtlCallable<T>> gets(@Nullable Collection<? extends Callable<T>> tasks, boolean releaseTtlValueReferenceAfterCall, boolean idempotent) {
        if (null == tasks) return Collections.emptyList();

        List<TtlCallable<T>> copy = new ArrayList<TtlCallable<T>>();
        for (Callable<T> task : tasks) {
            copy.add(TtlCallable.get(task, releaseTtlValueReferenceAfterCall, idempotent));
        }
        return copy;
    }

    /**
     * Unwrap {@link TtlCallable} to the original/underneath one.
     * <p>
     * this method is {@code null}-safe, when input {@code Callable} parameter is {@code null}, return {@code null};
     * if input {@code Callable} parameter is not a {@link TtlCallable} just return input {@code Callable}.
     * <p>
     * so {@code TtlCallable.unwrap(TtlCallable.get(callable))} will always return the same input {@code callable} object.
     *
     * @see #get(Callable)
     * @since 2.10.2
     */
    @Nullable
    public static <T> Callable<T> unwrap(@Nullable Callable<T> callable) {
        if (!(callable instanceof TtlCallable)) return callable;
        else return ((TtlCallable<T>) callable).getCallable();
    }

    /**
     * Unwrap {@link TtlCallable} to the original/underneath one.
     * <p>
     * Invoke {@link #unwrap(Callable)} for each element in input collection.
     * <p>
     * This method is {@code null}-safe, when input {@code Callable} parameter is {@code null}, return a empty list.
     *
     * @see #gets(Collection)
     * @see #unwrap(Callable)
     * @since 2.10.2
     */
    @NonNull
    public static <T> List<Callable<T>> unwraps(@Nullable Collection<? extends Callable<T>> tasks) {
        if (null == tasks) return Collections.emptyList();

        List<Callable<T>> copy = new ArrayList<Callable<T>>();
        for (Callable<T> task : tasks) {
            if (!(task instanceof TtlCallable)) copy.add(task);
            else copy.add(((TtlCallable<T>) task).getCallable());
        }
        return copy;
    }

    private final TtlAttachmentsDelegate ttlAttachment = new TtlAttachmentsDelegate();

    /**
     * see {@link TtlAttachments#setTtlAttachment(String, Object)}
     *
     * @since 2.11.0
     */
    @Override
    public void setTtlAttachment(@NonNull String key, Object value) {
        ttlAttachment.setTtlAttachment(key, value);
    }

    /**
     * see {@link TtlAttachments#getTtlAttachment(String)}
     *
     * @since 2.11.0
     */
    @Override
    public <T> T getTtlAttachment(@NonNull String key) {
        return ttlAttachment.getTtlAttachment(key);
    }
}
