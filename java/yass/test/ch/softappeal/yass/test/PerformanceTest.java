package ch.softappeal.yass.test;

import ch.softappeal.yass.Interceptor;
import ch.softappeal.yass.PerformanceTask;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PerformanceTest extends InvokeTest {

    public static PerformanceTask task(final TestService testService) {
        return new PerformanceTask() {
            @Override protected void run(final int count) throws Exception {
                var counter = count;
                while (counter-- > 0) {
                    if (testService.divide(12, 3) != 4) {
                        throw new RuntimeException();
                    }
                }
            }
        };
    }

    @Test public void direct() {
        task(new TestServiceImpl()).run(100_000, TimeUnit.NANOSECONDS);
    }

    private int counter;

    @Test public void proxy() {
        task(Interceptor.proxy(
            TestService.class,
            new TestServiceImpl(),
            (method, arguments, invocation) -> {
                counter++;
                return invocation.proceed();
            }
        )).run(100_000, TimeUnit.NANOSECONDS);
    }

}
