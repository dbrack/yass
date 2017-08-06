package ch.softappeal.yass.tutorial.contract.generic;

import ch.softappeal.yass.util.Nullable;

public final class Triple<F, T> extends Pair<F, Boolean> {

    public final @Nullable T third;

    private Triple() {
        third = null;
    }

    public Triple(final @Nullable F first, final @Nullable boolean second, final @Nullable T third) {
        super(first, second);
        this.third = third;
    }

}
