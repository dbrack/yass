package ch.softappeal.yass.serialize.test;

public class FieldModifiers {

    static boolean CONSTRUCTOR_CALLED;

    public static int STATIC_FIELD;

    public transient int transientField;

    private int privateField;

    private int privateFinalField;

    public int publicField;

    public final int publicFinalField;

    private FieldModifiers() {
        CONSTRUCTOR_CALLED = true;
        privateFinalField = 100;
        publicFinalField = 101;
    }

}
