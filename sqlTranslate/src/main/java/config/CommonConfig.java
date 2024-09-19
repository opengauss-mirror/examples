package config;

public class CommonConfig {
    public static final String DEFAULT_SOURCE_DB = "Oracle-19C";
    public static final String DEFAULT_TARGET_DB = "OpenGauss-3.0.0";
    private static String sourceDB = DEFAULT_SOURCE_DB;
    private static String targetDB = DEFAULT_TARGET_DB;

    public static void setSourceDB(String sourceDB) {
        CommonConfig.sourceDB = sourceDB;
    }

    public static void setTargetDB(String targetDB) {
        CommonConfig.targetDB = targetDB;
    }

    public static String getSourceDB() {
        return sourceDB;
    }

    public static String getTargetDB() {
        return targetDB;
    }

}
