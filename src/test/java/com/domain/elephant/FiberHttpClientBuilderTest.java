package com.domain.elephant;

import co.paralleluniverse.embedded.containers.EmbeddedServer;
import co.paralleluniverse.embedded.containers.JettyServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.httpclient.FiberHttpClientBuilder;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class FiberHttpClientBuilderTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {JettyServer.class},});
    }
    private final Class<? extends EmbeddedServer> cls;
    private EmbeddedServer server;

    public FiberHttpClientBuilderTest(Class<? extends EmbeddedServer> cls) {
        this.cls = cls;
    }

    @Before
    public void setUp() throws Exception {
        this.server = cls.newInstance();
        server.addServlet("test", TestServlet.class, "/");
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testConcurrency() throws IOException, InterruptedException, Exception {
        final int concurrencyLevel = 20;
        // snippet client configuration
        final CloseableHttpClient client = FiberHttpClientBuilder.
                create(2). // use 2 io threads
//                setProxy(new HttpHost("127.0.0.1", 8081)).
                setMaxConnPerRoute(concurrencyLevel).
                setMaxConnTotal(concurrencyLevel).build();
        // end of snippet
        final CountDownLatch cdl = new CountDownLatch(concurrencyLevel);
        LongAdder latency = new LongAdder();
        long t = System.currentTimeMillis();
        for (int i = 0; i < concurrencyLevel; i++) {
            int finalI = i;
            new Fiber<Void>(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    try {
                        long start = System.currentTimeMillis();
                        // snippet http call
//                        String response = client.execute(new HttpGet("https://www.cnblogs.com"), BASIC_RESPONSE_HANDLER);
//                        String response = client.execute(new HttpGet("http://localhost:8080"), BASIC_RESPONSE_HANDLER);
                        // end of snippet
//                        assertEquals("testGet", response);
//                        System.out.println(response);
                        start = System.currentTimeMillis() - start;
                        System.out.println(Thread.currentThread().getName() + "-" + finalI + ": http took " + start + "ms");

                        start = System.currentTimeMillis();
                        // snippet http call
                        String response = client.execute(new HttpGet("https://www.cnblogs.com"), BASIC_RESPONSE_HANDLER);
//                        String response = client.execute(new HttpGet("http://localhost:8080"), BASIC_RESPONSE_HANDLER);
                        // end of snippet
//                        assertEquals("testGet", response);
//                        System.out.println(response);
                        start = System.currentTimeMillis() - start;
                        System.out.println(Thread.currentThread().getName() + "-" + finalI + ": localhost took " + start + "ms");
                        latency.add(start);
                    } catch (IOException ex) {
                        fail(ex.getMessage());
                    } finally {
                        cdl.countDown();
                    }
                }
            }).start();
        }
//        cdl.await(5000, TimeUnit.MILLISECONDS);
//        assertEquals(0, cdl.getCount());
        cdl.await();
        t = System.currentTimeMillis() - t;
        long l = latency.longValue() / concurrencyLevel;
        System.out.println(Thread.currentThread().getName() + ": fiber took: " + t + ", latency: " + l + " ms");
        client.close();
    }

    public static class TestServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (PrintWriter out = resp.getWriter()) {
                Thread.sleep(500);
                out.print("testGet");
            } catch (InterruptedException ex) {
            }
        }
    }

    private static final BasicResponseHandler BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

}