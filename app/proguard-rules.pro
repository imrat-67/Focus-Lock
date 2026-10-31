# R8 keep rules (added as needed)

# Release builds carry no debug/info logging.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
