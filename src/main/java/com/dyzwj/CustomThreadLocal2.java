package com.dyzwj;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomThreadLocal2 {
    //    static ThreadLocal<String> threadLocal = new ThreadLocal<>();
//    static ThreadLocal<String> threadLocal = new InheritableThreadLocal<>();
//    static ExecutorService executorService = Executors.newFixedThreadPool(2);
//    static ExecutorService executorService = Executors.newFixedThreadPool(100);
    static TransmittableThreadLocal<String> threadLocal = new TransmittableThreadLocal<>();
    static ExecutorService executorService =  TtlExecutors.getTtlExecutorService(Executors.newFixedThreadPool(2));

    public static void main(String[] args) {
//        CustomThreadLocal2.threadLocal.set("猿天地");
        for (int i = 0; i < 100; i++) {
            int j = i;
            executorService.execute(new Runnable() {

                public void run() {
                    /**
                     *  添加到全局holder
                     */
                    CustomThreadLocal2.threadLocal.set("猿天地"+j);

                    new Service1().call();
                }
            });
        }
    }
}
class Service1 {
    public void call() {
        /**
         *  ExecutorTtlWrapper#execute()将Runnable包装为TtlRunnable
         *  TtlRunnable构造方法中将当前全局holder里的变量存储在当前对象的capturedRef属性
         */
        CustomThreadLocal2.executorService.execute(new Runnable() {
            public void run() {

                new Dao1().call();
            }
        });

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                new Dao1().call();
//            }
//        }).start();
    }
}
class Dao1 {
    public void call() {
        System.out.println("Dao:" + CustomThreadLocal2.threadLocal.get());
    }
}

