package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

@Tag(16) public final class Node {

    @Tag(1) public int id;
    @Tag(2) @Nullable public Node link;

    public Node(final int id) {
        this.id = id;
    }

}