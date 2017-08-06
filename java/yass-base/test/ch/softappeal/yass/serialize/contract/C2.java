package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

@Tag(120) public final class C2 {

    @Tag(1) public final int i1;
    @Tag(2) public final @Nullable Integer i2;

    private C2() {
        i1 = 0;
        i2 = null;
    }

    public C2(final int i1, final int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public int i2() {
        return (i2 == null) ? 13 : i2;
    }

}
