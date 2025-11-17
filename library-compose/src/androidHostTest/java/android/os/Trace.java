package android.os;

public final class Trace {
    private Trace() {}

    public static void beginSection(String sectionName) {
        // no-op for host tests
    }

    public static void endSection() {
        // no-op for host tests
    }

    public static boolean isEnabled() {
        return false;
    }
}
