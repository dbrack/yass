package ch.softappeal.yass.test;

import ch.softappeal.yass.Dumper;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.TestUtils;
import ch.softappeal.yass.serialize.test.SerializerTest;
import org.junit.Test;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class DumperTest {

    public static final class TestDumper extends Dumper {
        public TestDumper(final boolean compact, final boolean referenceables, final Class<?>... concreteValueClasses) {
            super(compact, referenceables, concreteValueClasses);
        }
        @Override protected boolean dumpValueClass(final StringBuilder out, final Class<?> type, final Object object) {
            if (isConcreteValueClass(type) || (Date.class == type) || (BigInteger.class == type) || (BigDecimal.class == type) || (Instant.class == type)) {
                out.append(object);
                return true;
            }
            return false;
        }
    }

    @Test public void test() {
        final var timeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            TestUtils.compareFile("ch/softappeal/yass/test/DumperTest.dump.txt", new TestUtils.Printer() {
                void dump(final Dumper dumper, final StringBuilder s, final @Nullable Object value) {
                    s.append(dumper.dump(value)).append('\n');
                }
                void print(final Dumper dumper, final PrintWriter printer, final boolean cycles) {
                    final var s = new StringBuilder(1024);
                    dump(dumper, s, null);
                    dump(dumper, s, 'c');
                    dump(dumper, s, SerializerTest.createNulls());
                    dump(dumper, s, SerializerTest.createValues());
                    if (cycles) {
                        dump(dumper, s, SerializerTest.createGraph());
                    }
                    dump(dumper, s, new Object[] {"one", "two", "three"});
                    final Map<Integer, String> int2string = new LinkedHashMap<>();
                    int2string.put(1, "one");
                    int2string.put(2, null);
                    int2string.put(3, "three");
                    dump(dumper, s, int2string);
                    printer.append(s);
                }
                @Override public void print(final PrintWriter printer) {
                    print(new TestDumper(false, true, BigInteger.class, BigDecimal.class, Instant.class), printer, true);
                    print(new TestDumper(false, false), printer, false);
                    print(new TestDumper(true, true, BigInteger.class, BigDecimal.class, Instant.class), printer, true);
                    print(new TestDumper(true, false), printer, false);
                }
            });
        } finally {
            TimeZone.setDefault(timeZone);
        }
    }

}
