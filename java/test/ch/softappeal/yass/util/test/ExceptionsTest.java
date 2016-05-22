package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Exceptions;
import org.junit.Assert;
import org.junit.Test;

public class ExceptionsTest {

    @Test public void stdErrException() {
        try {
            throw new Exception("Test");
        } catch (final Exception e) {
            Exceptions.uncaughtException(Exceptions.STD_ERR, e);
        }
    }

    @Test public void wrap1() {
        final Exception e = new RuntimeException();
        Assert.assertSame(e, Exceptions.wrap(e));
    }

    @Test public void wrap2() {
        final Exception e = new Exception();
        Assert.assertSame(e, Exceptions.wrap(e).getCause());
    }

    public static void main(final String... args) {
        Exceptions.STD_ERR.uncaughtException(null, new Throwable());
    }

}
