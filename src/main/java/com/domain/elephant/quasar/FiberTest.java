package com.domain.elephant.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
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

        String m = "m1";
//        System.out.println("m1 begin");
        m = m2();
//        System.out.println("m1 end");
//        System.out.println(m);
    }

    @Suspendable
    private static String m2() throws InterruptedException {

        String m = m3();
        try {
            Strand.sleep(1000);
        } catch (SuspendExecution suspendExecution) {
            suspendExecution.printStackTrace();
        }
        return m;
    }

    // or define in META-INF/suspendables
    @Suspendable
    private static String m3() {

        List l = Stream.of(1, 2, 3).filter(i -> i % 2 == 0).collect(Collectors.toList());
        return l.toString();
    }

    static public void main(String[] args) throws InterruptedException {

        int count = 10;
        testThreadPool(count);
        testFiber(count);
        testFiberHttp(count);
    }

    private static void testThreadPool(int count) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(count);
        ExecutorService es = Executors.newFixedThreadPool(200);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            es.submit(() -> {
                System.out.println(Thread.currentThread().getName());

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
                System.out.println(Thread.currentThread().getName());

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

    private static int concurrencyLevel = 500;

    private static void testFiberHttp(int count) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(count);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            new Fiber<Void>("Http Caller", (SuspendableRunnable) () -> {
                System.out.println(Thread.currentThread().getName());

                final CloseableHttpClient client = FiberHttpClientBuilder.
                        create(2). // use 2 io threads
                        setMaxConnPerRoute(concurrencyLevel).
                        setMaxConnTotal(concurrencyLevel).build();

                long start = System.currentTimeMillis();
                try {
                    m11(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Suspendable
    private static void m11(CloseableHttpClient client) throws IOException {

        long start = System.currentTimeMillis();
        CloseableHttpResponse response = client.execute(new HttpGet("http://www.baidu.com"));
        start = System.currentTimeMillis() - start;
        System.out.println("http took " + start + "ms");
    }
}
