package com.alibaba.ttl.threadpool;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.spi.TtlEnhanced;
import com.alibaba.ttl.spi.TtlWrapper;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.concurrent.Executor;

/**
 * {@link TransmittableThreadLocal} Wrapper of {@link Executor},
 * transmit the {@link TransmittableThreadLocal} from the task submit time of {@link Runnable}
 * to the execution time of {@link Runnable}.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @since 0.9.0
 */
class ExecutorTtlWrapper implements Executor, TtlWrapper<Executor>, TtlEnhanced {
    private final Executor executor;

    ExecutorTtlWrapper(@NonNull Executor executor) {
        this.executor = executor;
    }

    /**
     * 这里会把Rannable包装一层，这是关键，有些逻辑处理，需要在run之前执行
     *
     */
    @Override
    public void execute(@NonNull Runnable command) {
        //将Runnable包装为TtlRunnable   注：此时的线程可能是用户线程 也可能是线程池的线程
        //每个Runnable都会包装为TtlRunnable
        executor.execute(TtlRunnable.get(command));
    }

    @Override
    @NonNull
    public Executor unwrap() {
        return executor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutorTtlWrapper that = (ExecutorTtlWrapper) o;

        return executor.equals(that.executor);
    }

    @Override
    public int hashCode() {
        return executor.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " - " + executor.toString();
    }
}
