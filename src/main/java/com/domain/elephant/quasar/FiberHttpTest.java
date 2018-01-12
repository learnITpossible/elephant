package com.domain.elephant.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

/**
 * com.domain.elephant.quasar
 * @author Mark Li
 * @version 1.0.0
 * @since 2018/1/10
 */
public class FiberHttpTest {

    static public void main(String[] args) throws InterruptedException {

        int count = 50;
        testThreadPoolHttp(count);
        //        testFiberHttp(count);
    }

    private static void testThreadPoolHttp(int count) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(count);
        ExecutorService es = Executors.newFixedThreadPool(200);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            es.submit(() -> {
                //                System.out.println(Thread.currentThread().getName());

                try {
                    long start = System.currentTimeMillis();
                    try {
                        m12();
                    } catch (IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    start = System.currentTimeMillis() - start;
                    latency.add(start);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        t = System.currentTimeMillis() - t;
        long l = latency.longValue() / count;
        System.out.println(Thread.currentThread().getName() + ": thread pool took: " + t + ", latency: " + l + " ms");
        es.shutdownNow();
    }

    private static int concurrencyLevel = 500;

    private static void testFiberHttp(int count) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(count);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {

            new Fiber<Void>((SuspendableRunnable) () -> {
                //                System.out.println(Thread.currentThread().getName());

                try {
                    long start = System.currentTimeMillis();
                    try {
                        m11();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    start = System.currentTimeMillis() - start;
                    latency.add(start);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        t = System.currentTimeMillis() - t;
        long l = latency.longValue() / count;
        System.out.println(Thread.currentThread().getName() + ": fiber took: " + t + ", latency: " + l + " ms");
    }

    @Suspendable
    private static void m11() throws Exception {

        long start = System.currentTimeMillis();

        CloseableHttpClient client = getFiberHttpClient();

        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(Thread.currentThread().getName() + ": get http client " + dur + "ms");

        CloseableHttpResponse response = client.execute(new HttpGet("http://www.baidu.com/"));

        //        System.out.println(EntityUtils.toString(response.getEntity()));
        dur = System.currentTimeMillis() - end;
        System.out.println(Thread.currentThread().getName() + ": http took " + dur + "ms");

        response.close();
        //        client.close();
    }

    private static CloseableHttpClient N_client = null;

    private static CloseableHttpClient getFiberHttpClient() throws Exception {

        if (N_client == null) {
            synchronized (FiberHttpTest.class) {
                if (N_client == null) {
                    Registry<SchemeIOSessionStrategy> defaultRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                            .register("http", NoopIOSessionStrategy.INSTANCE)
                            .register("https", new SSLIOSessionStrategy(
                                    SSLContexts.createDefault(), null, null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER))
                            .build();
                    PoolingNHttpClientConnectionManager N_connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor(), null, defaultRegistry);
                    N_connectionManager.setMaxTotal(200);
                    N_connectionManager.setDefaultMaxPerRoute(20);
                    N_client = FiberHttpClientBuilder.create()
                            .setConnectionManager(N_connectionManager)
                            //                .setMaxConnPerRoute(200)
                            //                .setMaxConnTotal(20)
                            .build();
                }
            }
        }

        // can't use pool, because if http requests are more than pool max size,
        // then extra http requests will 'I/O reactor terminated abnormally'
        TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] certificate, String authType) {

                return true;
            }
        };
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy).build();

        return N_client;
    }

    private static void m12() throws IOException, NoSuchAlgorithmException {

        long start = System.currentTimeMillis();

        CloseableHttpClient client = getHttpClient();

        long end = System.currentTimeMillis();
        long dur = end - start;
        System.out.println(Thread.currentThread().getName() + ": get http client " + dur + "ms");

        CloseableHttpResponse response = client.execute(new HttpGet("https://www.baidu.com/"));

        //        System.out.println(EntityUtils.toString(response.getEntity()));
        dur = System.currentTimeMillis() - end;
        System.out.println(Thread.currentThread().getName() + ": http took " + dur + "ms");

        response.close();
    }

    private static CloseableHttpClient client = null;

    private static CloseableHttpClient getHttpClient() throws NoSuchAlgorithmException {

        if (client == null) {
            synchronized (FiberHttpTest.class) {
                if (client == null) {
                    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("https", new SSLConnectionSocketFactory(SSLContext.getDefault()))
                            .register("http", new PlainConnectionSocketFactory())
                            .build();
                    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                    connectionManager.setMaxTotal(200);
                    connectionManager.setDefaultMaxPerRoute(20);

                    client = HttpClientBuilder.create()
                            .setConnectionManager(connectionManager)
                            //                            .setMaxConnPerRoute(concurrencyLevel)
                            //                            .setMaxConnTotal(concurrencyLevel)
                            .build();
                }
            }
        }
        return client;
    }
}
