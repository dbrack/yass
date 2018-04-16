package ch.softappeal.yass.test;

import ch.softappeal.yass.Reference;
import org.junit.Assert;
import org.junit.Test;

public class ReferenceTest {

    @Test public void notNullReference() {
        final var v = "value";
        final var s = Reference.create(v);
        Assert.assertSame(v, s.get());
        Assert.assertFalse(s.isNull());
        Assert.assertEquals(v, s.toString());
        s.setNull();
        Assert.assertNull(s.get());
        s.set(v);
        Assert.assertSame(v, s.get());
    }

    @Test public void nullReference() {
        final Reference<String> s = Reference.create();
        Assert.assertNull(s.get());
        Assert.assertTrue(s.isNull());
        Assert.assertEquals("null", s.toString());
    }

}
