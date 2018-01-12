package com.domain.elephant.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * com.domain.elephant.quasar
 * @author Mark Li
 * @version 1.0.0
 * @since 2018/1/10
 */
public class FiberTest {

    @Suspendable
    private static void m1() throws InterruptedException {

        long start = System.currentTimeMillis();
        String m = "m1";
        //        System.out.println("m1 begin");
        m = m2();
        //        System.out.println("m1 end");
        //        System.out.println(m);
        start = System.currentTimeMillis() - start;
        System.out.println(Thread.currentThread().getName() + ": m1 took: " + start + "ms");
    }

    @Suspendable
    private static String m2() throws InterruptedException {

        String m = m3();
        try {
            Strand.sleep(1000);
        } catch (SuspendExecution suspendExecution) {
            suspendExecution.printStackTrace();
        }
        // Thread.sleep will block thread
        // Thread.sleep(1000);
        return m;
    }

    // or define in META-INF/suspendables
    @Suspendable
    private static String m3() {

        List l = Stream.of(1, 2, 3).filter(i -> i % 2 == 0).collect(Collectors.toList());
        return l.toString();
    }

    static public void main(String[] args) throws InterruptedException {

        int count = 1000;
        testThreadPool(count);
        testFiber(count);
    }

    private static void testThreadPool(int count) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(count);
        ExecutorService es = Executors.newFixedThreadPool(200);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            es.submit(() -> {
                //                System.out.println(Thread.currentThread().getName());

                long start = System.currentTimeMillis();
                try {
                    m1();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                start = System.currentTimeMillis() - start;
                latency.add(start);
                latch.countDown();
            });
        }
        latch.await();
        t = System.currentTimeMillis() - t;
        long l = latency.longValue() / count;
        System.out.println("thread pool took: " + t + ", latency: " + l + " ms");
        es.shutdownNow();
    }

    private static void testFiber(int count) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(count);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            new Fiber<Void>("Caller", (SuspendableRunnable) () -> {
                //                System.out.println(Thread.currentThread().getName());

                long start = System.currentTimeMillis();
                m1();
                start = System.currentTimeMillis() - start;
                latency.add(start);
                latch.countDown();
            }).start();
        }
        latch.await();
        t = System.currentTimeMillis() - t;
        long l = latency.longValue() / count;
        System.out.println("fiber took: " + t + ", latency: " + l + " ms");
    }
}
