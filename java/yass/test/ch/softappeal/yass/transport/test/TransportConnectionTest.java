package ch.softappeal.yass.transport.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TransportConnectionTest extends TransportTest {

    @Test public void plain1() throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            TransportConnection.connect(invokeTransportSetup(true, false, executor), invokeTransportSetup(false, false, executor));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void plain2() throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            TransportConnection.connect(invokeTransportSetup(false, false, executor), invokeTransportSetup(true, false, executor));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void createException() {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            try {
                TransportConnection.connect(invokeTransportSetup(false, false, executor), invokeTransportSetup(false, true, executor));
                Assert.fail();
            } catch (final RuntimeException e) {
                Assert.assertEquals(e.getMessage(), "java.lang.Exception: create failed");
            }
        } finally {
            executor.shutdown();
        }
    }

}
